package com.aton.proj.oneGasMeter.server;

import com.aton.proj.oneGasMeter.config.DlmsSessionConfig;
import com.aton.proj.oneGasMeter.cosem.CosemObject;
import com.aton.proj.oneGasMeter.dlms.DlmsMeterClient;
import com.aton.proj.oneGasMeter.dlms.IncomingTcpTransport;
import com.aton.proj.oneGasMeter.entity.CommandType;
import com.aton.proj.oneGasMeter.entity.DeviceCommand;
import com.aton.proj.oneGasMeter.exception.DlmsCommunicationException;
import com.aton.proj.oneGasMeter.service.CommandService;
import com.aton.proj.oneGasMeter.service.TelemetryService;
import gurux.dlms.objects.GXDLMSClock;
import gurux.dlms.objects.GXDLMSRegister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Socket;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Gestisce una singola sessione di comunicazione con un contatore gas.
 *
 * Flusso lineare:
 * 1. Connessione DLMS (handshake AARQ/AARE)
 * 2. Lettura numero seriale
 * 3. Lettura orologio del contatore
 * 4. Lettura di tutti gli oggetti COSEM auto-leggibili
 * 5. Salvataggio dati in database
 * 6. Esecuzione comandi pendenti
 * 7. Disconnessione
 */
public class MeterSessionHandler {

    private static final Logger log = LoggerFactory.getLogger(MeterSessionHandler.class);

    private final Socket socket;
    private final DlmsSessionConfig dlmsConfig;
    private final int timeoutMs;
    private final TelemetryService telemetryService;
    private final CommandService commandService;

    public MeterSessionHandler(Socket socket, DlmsSessionConfig dlmsConfig, int timeoutMs,
                                TelemetryService telemetryService, CommandService commandService) {
        this.socket = socket;
        this.dlmsConfig = dlmsConfig;
        this.timeoutMs = timeoutMs;
        this.telemetryService = telemetryService;
        this.commandService = commandService;
    }

    /**
     * Esegue la sessione completa con il contatore.
     */
    public void handle() {
        String meterIp = socket.getInetAddress().getHostAddress();
        String sessionId = UUID.randomUUID().toString();
        DlmsMeterClient client = null;

        try {
            // 1. Crea il client DLMS e connetti
            IncomingTcpTransport transport = new IncomingTcpTransport(socket, timeoutMs);
            client = new DlmsMeterClient(transport, dlmsConfig);
            client.connect();
            log.info("[{}] Sessione DLMS avviata da {}", sessionId, meterIp);

            // 2. Leggi il numero seriale
            String serialNumber = readSerialNumber(client);
            log.info("[{}] Contatore identificato: {}", sessionId, serialNumber);

            // 3. Leggi l'orologio del contatore
            Instant meterTimestamp = readMeterClock(client);
            log.info("[{}] Orologio contatore: {}", sessionId, meterTimestamp);

            // 4. Leggi e salva tutti gli oggetti COSEM auto-leggibili
            readAndSaveCosemObjects(client, serialNumber, meterIp, sessionId, meterTimestamp);

            // 5. Esegui comandi pendenti
            executePendingCommands(client, serialNumber, sessionId);

            log.info("[{}] Sessione completata per contatore {}", sessionId, serialNumber);

        } catch (Exception e) {
            log.error("[{}] Errore durante la sessione da {}: {}", sessionId, meterIp, e.getMessage(), e);
        } finally {
            // 6. Disconnetti
            if (client != null) {
                client.disconnect();
            }
        }
    }

    /**
     * Legge il numero seriale dal contatore (OBIS 0.0.96.1.0.255).
     */
    private String readSerialNumber(DlmsMeterClient client) {
        try {
            Object value = client.readData(CosemObject.SERIAL_NUMBER.getObisCode());
            if (value == null) {
                throw new DlmsCommunicationException("Numero seriale null");
            }
            // Il seriale puo' essere bytes o stringa
            if (value instanceof byte[]) {
                return new String((byte[]) value).trim();
            }
            return String.valueOf(value).trim();
        } catch (DlmsCommunicationException e) {
            throw new DlmsCommunicationException("Impossibile leggere il numero seriale", e);
        }
    }

    /**
     * Legge l'orologio del contatore e lo converte in Instant.
     */
    private Instant readMeterClock(DlmsMeterClient client) {
        try {
            GXDLMSClock clock = client.readClock();
            if (clock.getTime() != null && clock.getTime().getMeterCalendar() != null) {
                return clock.getTime().getMeterCalendar().toInstant();
            }
            log.info("Impossibile leggere l'orologio del contatore, uso ora server");
            return Instant.now(); // fallback se il clock non e' disponibile
        } catch (DlmsCommunicationException e) {
            log.warn("Impossibile leggere l'orologio del contatore, uso ora server: {}", e.getMessage());
            return Instant.now();
        }
    }

    /**
     * Legge tutti gli oggetti COSEM marcati come auto-leggibili e salva in DB.
     * Se la lettura di un singolo oggetto fallisce, continua con gli altri.
     */
    private void readAndSaveCosemObjects(DlmsMeterClient client, String serialNumber,
                                          String meterIp, String sessionId, Instant meterTimestamp) {
        int successCount = 0;
        int errorCount = 0;

        for (CosemObject cosemObj : CosemObject.values()) {
            if (!cosemObj.isAutoReadable()) {
                continue;
            }
            // Il serial number e il clock sono gia' stati letti
            if (cosemObj == CosemObject.SERIAL_NUMBER || cosemObj == CosemObject.CLOCK) {
                continue;
            }

            try {
                readSingleCosemObject(client, cosemObj, serialNumber, meterIp, sessionId, meterTimestamp);
                successCount++;
            } catch (Exception e) {
                errorCount++;
                log.warn("Errore lettura {} ({}): {}", cosemObj.name(), cosemObj.getObisCode(), e.getMessage());
            }
        }

        log.info("[{}] Letture completate: {} successo, {} errori", sessionId, successCount, errorCount);
    }

    /**
     * Legge un singolo oggetto COSEM e lo salva in DB.
     */
    private void readSingleCosemObject(DlmsMeterClient client, CosemObject cosemObj,
                                        String serialNumber, String meterIp,
                                        String sessionId, Instant meterTimestamp) {
        int classId = cosemObj.getClassId();

        if (classId == 1) {
            // Data
            Object value = client.readData(cosemObj.getObisCode());
            telemetryService.save(serialNumber, meterIp, sessionId,
                    cosemObj.getObisCode(), classId, value, 0, null, meterTimestamp);

        } else if (classId == 3) {
            // Register
            GXDLMSRegister reg = client.readRegister(cosemObj.getObisCode());
            String unit = reg.getUnit() != null ? reg.getUnit().toString() : null;
            telemetryService.save(serialNumber, meterIp, sessionId,
                    cosemObj.getObisCode(), classId, reg.getValue(), reg.getScaler(), unit, meterTimestamp);

        } else if (classId == 70) {
            // DisconnectControl - leggi come Data
            Object value = client.readData(cosemObj.getObisCode());
            telemetryService.save(serialNumber, meterIp, sessionId,
                    cosemObj.getObisCode(), classId, value, 0, null, meterTimestamp);

        } else {
            log.debug("Class ID {} non gestito per auto-lettura: {}", classId, cosemObj.getObisCode());
        }
    }

    /**
     * Cerca e esegue i comandi pendenti per il contatore.
     * Per aggiungere un nuovo tipo di comando:
     * 1. Aggiungere il valore in CommandType
     * 2. Aggiungere il case nello switch qui sotto
     */
    private void executePendingCommands(DlmsMeterClient client, String serialNumber, String sessionId) {
        List<DeviceCommand> pendingCommands = commandService.getPendingCommands(serialNumber);

        if (pendingCommands.isEmpty()) {
            log.debug("[{}] Nessun comando pendente per {}", sessionId, serialNumber);
            return;
        }

        log.info("[{}] {} comandi pendenti per {}", sessionId, pendingCommands.size(), serialNumber);

        for (DeviceCommand command : pendingCommands) {
            commandService.markInProgress(command);
            try {
                executeCommand(client, command);
                commandService.markDone(command);
            } catch (Exception e) {
                commandService.markFailed(command, e.getMessage());
                log.error("[{}] Comando {} fallito per {}: {}",
                        sessionId, command.getCommandType(), serialNumber, e.getMessage());
            }
        }
    }

    /**
     * Esegue un singolo comando sul contatore.
     *
     * @param client  client DLMS connesso
     * @param command comando da eseguire
     */
    private void executeCommand(DlmsMeterClient client, DeviceCommand command) {
        CommandType type = command.getCommandType();

        if (type == CommandType.SYNC_CLOCK) {
            client.syncClock();

        } else if (type == CommandType.SET_CLOCK) {
            Instant timestamp = Instant.parse(command.getPayload());
            client.setClock(timestamp);

        } else if (type == CommandType.DISCONNECT_VALVE) {
            client.disconnectValve();

        } else if (type == CommandType.RECONNECT_VALVE) {
            client.reconnectValve();

        } else if (type == CommandType.READ_LOAD_PROFILE) {
            log.info("Lettura profilo di carico richiesta per {}", command.getSerialNumber());
            // TODO: implementare lettura selettiva del profilo con date dal payload JSON

        } else if (type == CommandType.READ_EVENT_LOG) {
            log.info("Lettura log eventi richiesta per {}", command.getSerialNumber());
            // TODO: implementare lettura log eventi

        } else if (type == CommandType.CHANGE_PUSH_DESTINATION) {
            log.info("Cambio destinazione push richiesto per {}", command.getSerialNumber());
            // TODO: implementare cambio destinazione push con ip/port dal payload JSON

        } else {
            throw new DlmsCommunicationException(
                    "Tipo di comando non supportato: " + command.getCommandType());
        }
    }
}
