package com.aton.proj.oneGasMeter.server;

import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aton.proj.oneGasMeter.config.DlmsSessionConfig;
import com.aton.proj.oneGasMeter.cosem.CosemObject;
import com.aton.proj.oneGasMeter.dlms.DlmsMeterClient;
import com.aton.proj.oneGasMeter.dlms.IncomingTcpTransport;
import com.aton.proj.oneGasMeter.entity.CommandType;
import com.aton.proj.oneGasMeter.entity.DeviceCommand;
import com.aton.proj.oneGasMeter.exception.DlmsCommunicationException;
import com.aton.proj.oneGasMeter.service.CommandService;
import com.aton.proj.oneGasMeter.service.TelemetryService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import gurux.dlms.objects.GXDLMSClock;
import gurux.dlms.objects.GXDLMSProfileGeneric;
import gurux.dlms.objects.GXDLMSRegister;

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
    private static final ObjectMapper objectMapper = new ObjectMapper();

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
            } else {
                // Se client e' null, il transport/socket non e' stato chiuso da disconnect()
                try {
                    socket.close();
                } catch (Exception e) {
                    log.debug("Errore chiusura socket orfano: {}", e.getMessage());
                }
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
                return new String((byte[]) value, StandardCharsets.UTF_8).trim();
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
            var time = clock.getTime();
            if (time != null && time.getMeterCalendar() != null) {
                return time.getMeterCalendar().toInstant();
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
     * Estrae ip e port dal payload JSON di CHANGE_PUSH_DESTINATION.
     * Formato atteso: {"ip":"10.0.0.1","port":4059}
     *
     * @return array [ip, port] come stringhe
     */
    static String[] parsePushDestinationPayload(String payload) {
        try {
            JsonNode json = objectMapper.readTree(payload);
            JsonNode ipNode = json.get("ip");
            JsonNode portNode = json.get("port");
            if (ipNode == null || portNode == null) {
                throw new DlmsCommunicationException(
                        "Payload CHANGE_PUSH_DESTINATION invalido, servono 'ip' e 'port': " + payload);
            }
            return new String[]{ipNode.asText(), portNode.asText()};
        } catch (DlmsCommunicationException e) {
            throw e;
        } catch (Exception e) {
            throw new DlmsCommunicationException("Errore parsing payload CHANGE_PUSH_DESTINATION: " + payload, e);
        }
    }

    /**
     * Determina l'OBIS code del profilo di carico dal payload JSON.
     * Formato atteso: {"profile":"daily"} o {"profile":"monthly"}.
     * Se non specificato, default a LOAD_PROFILE_1 (giornaliero).
     *
     * @return OBIS code del profilo selezionato
     */
    static String resolveLoadProfileObis(String payload) {
        if (payload == null || payload.isBlank()) {
            return CosemObject.DAILY_LOAD_PROFILE.getObisCode();
        }
        try {
            JsonNode json = objectMapper.readTree(payload);
            String profile = json.has("profile") ? json.get("profile").asText() : "daily";
            return switch (profile) {
                case "monthly" -> CosemObject.SNAPSHOT_PERIOD_DATA.getObisCode();
                default -> CosemObject.DAILY_LOAD_PROFILE.getObisCode();
            };
        } catch (Exception e) {
            return CosemObject.DAILY_LOAD_PROFILE.getObisCode();
        }
    }

    /**
     * Estrae un range temporale opzionale dal payload JSON.
     * Formato atteso: {"from":"2026-01-01T00:00:00Z","to":"2026-03-28T00:00:00Z"}
     * Se il payload e' null/vuoto, restituisce [null, null] (lettura completa).
     *
     * @return array [from, to] come Date (entrambi possono essere null)
     */
    static Date[] parseDateRangePayload(String payload) {
        if (payload == null || payload.isBlank()) {
            return new Date[]{null, null};
        }
        try {
            JsonNode json = objectMapper.readTree(payload);
            Date from = json.has("from") ? Date.from(Instant.parse(json.get("from").asText())) : null;
            Date to = json.has("to") ? Date.from(Instant.parse(json.get("to").asText())) : null;
            return new Date[]{from, to};
        } catch (Exception e) {
            throw new DlmsCommunicationException("Errore parsing payload date range: " + payload, e);
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
            String payload = command.getPayload();
            if (payload == null || payload.isBlank()) {
                throw new DlmsCommunicationException("Payload mancante per comando SET_CLOCK");
            }
            Instant timestamp = Instant.parse(payload);
            client.setClock(timestamp);

        } else if (type == CommandType.DISCONNECT_VALVE) {
            client.disconnectValve();

        } else if (type == CommandType.RECONNECT_VALVE) {
            client.reconnectValve();

        } else if (type == CommandType.READ_LOAD_PROFILE) {
            String payload = command.getPayload();
            Date[] range = parseDateRangePayload(payload);
            String obisCode = resolveLoadProfileObis(payload);
            GXDLMSProfileGeneric profile = client.readProfileGeneric(obisCode, range[0], range[1]);
            log.info("Load profile ({}) letto: {} righe", obisCode,
                    profile.getBuffer() != null ? profile.getBuffer().length : 0);

        } else if (type == CommandType.READ_EVENT_LOG) {
            Date[] range = parseDateRangePayload(command.getPayload());
            GXDLMSProfileGeneric profile = client.readProfileGeneric(
                    CosemObject.METROLOGICAL_LOGBOOK.getObisCode(), range[0], range[1]);
            log.info("Event log letto: {} righe",
                    profile.getBuffer() != null ? profile.getBuffer().length : 0);

        } else if (type == CommandType.CHANGE_PUSH_DESTINATION) {
            String payload = command.getPayload();
            if (payload == null || payload.isBlank()) {
                throw new DlmsCommunicationException("Payload mancante per comando CHANGE_PUSH_DESTINATION");
            }
            String[] parts = parsePushDestinationPayload(payload);
            client.setPushDestination(parts[0], Integer.parseInt(parts[1]));

        } else {
            throw new DlmsCommunicationException(
                    "Tipo di comando non supportato: " + command.getCommandType());
        }
    }
}
