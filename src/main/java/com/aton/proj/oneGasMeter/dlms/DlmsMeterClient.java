package com.aton.proj.oneGasMeter.dlms;

import com.aton.proj.oneGasMeter.config.DlmsSessionConfig;
import com.aton.proj.oneGasMeter.cosem.CosemObject;
import com.aton.proj.oneGasMeter.exception.DlmsCommunicationException;
import gurux.dlms.GXDLMSClient;
import gurux.dlms.GXReplyData;
import gurux.dlms.enums.*;
import gurux.dlms.objects.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;
import java.util.Date;

/**
 * Client DLMS/COSEM semplificato per comunicazione TCP inbound (WRAPPER).
 * Gestisce l'handshake AARQ/AARE, la lettura dei dati e l'invio di comandi
 * verso i contatori gas italiani.
 *
 * Uso tipico:
 * <pre>
 *   DlmsMeterClient client = new DlmsMeterClient(socket, config, timeoutMs);
 *   client.connect();
 *   Object serial = client.readData("0.0.96.1.0.255");
 *   client.disconnect();
 * </pre>
 */
public class DlmsMeterClient {

    private static final Logger log = LoggerFactory.getLogger(DlmsMeterClient.class);

    private final DlmsTransport transport;
    private final GXDLMSClient gxClient;
    private boolean connected = false;

    /**
     * Crea un client DLMS su un socket TCP gia' connesso.
     *
     * @param transport  trasporto DLMS (tipicamente IncomingTcpTransport)
     * @param config     configurazione sessione DLMS
     */
    public DlmsMeterClient(DlmsTransport transport, DlmsSessionConfig config) {
        this.transport = transport;
        this.gxClient = new GXDLMSClient(
                config.isUseLogicalNameReferencing(),
                config.getClientAddress(),
                config.getServerAddress(),
                mapAuthentication(config.getAuthentication()),
                null,
                InterfaceType.WRAPPER
        );
        // Imposta la password se presente
        if (config.getPassword() != null && !config.getPassword().isEmpty()) {
            gxClient.setPassword(config.getPassword().getBytes());
        }
    }

    /**
     * Crea un client DLMS con un GXDLMSClient custom (per test).
     */
    DlmsMeterClient(DlmsTransport transport, GXDLMSClient gxClient) {
        this.transport = transport;
        this.gxClient = gxClient;
    }

    /**
     * Esegue l'handshake DLMS (SNRM/AARQ -> UA/AARE).
     *
     * @throws DlmsCommunicationException se l'handshake fallisce
     */
    public void connect() {
        try {
            log.info("Avvio handshake DLMS...");

            // Per WRAPPER, non serve SNRM (solo HDLC). Si passa direttamente all'AARQ.
            byte[][] aarqData = gxClient.aarqRequest();
            GXReplyData reply = new GXReplyData();
            for (byte[] frame : aarqData) {
                reply.clear();
                sendReceive(frame, reply);
            }
            gxClient.parseAareResponse(reply.getData());

            connected = true;
            log.info("Handshake DLMS completato. Autenticazione: {}", gxClient.getAuthentication());

        } catch (Exception e) {
            throw new DlmsCommunicationException("Errore durante l'handshake DLMS", e);
        }
    }

    /**
     * Chiude la connessione DLMS inviando un release request.
     */
    public void disconnect() {
        try {
            if (connected) {
                // Invia release request
                byte[][] releaseData = gxClient.releaseRequest();
                if (releaseData != null) {
                    GXReplyData reply = new GXReplyData();
                    for (byte[] frame : releaseData) {
                        reply.clear();
                        try {
                            sendReceive(frame, reply);
                        } catch (Exception e) {
                            log.debug("Errore durante release (ignorato): {}", e.getMessage());
                        }
                    }
                }
                connected = false;
            }
        } catch (Exception e) {
            log.debug("Errore durante disconnect (ignorato): {}", e.getMessage());
        } finally {
            transport.close();
        }
    }

    /**
     * Legge un oggetto Data (Class ID 1) per codice OBIS.
     *
     * @param obisCode codice OBIS (es. "0.0.96.1.0.255")
     * @return il valore dell'attributo 2 (value)
     */
    public Object readData(String obisCode) {
        GXDLMSData obj = new GXDLMSData(obisCode);
        readObject(obj, 2);
        return obj.getValue();
    }

    /**
     * Legge un oggetto Register (Class ID 3) per codice OBIS.
     *
     * @param obisCode codice OBIS
     * @return il registro con value, scaler e unit
     */
    public GXDLMSRegister readRegister(String obisCode) {
        GXDLMSRegister obj = new GXDLMSRegister(obisCode);
        // Leggi attributo 3 (scaler_unit) prima del valore
        readObject(obj, 3);
        // Leggi attributo 2 (value)
        readObject(obj, 2);
        return obj;
    }

    /**
     * Legge l'orologio del contatore (Class ID 8, OBIS 0.0.1.0.0.255).
     *
     * @return l'oggetto Clock con la data/ora del contatore
     */
    public GXDLMSClock readClock() {
        GXDLMSClock obj = new GXDLMSClock(CosemObject.CLOCK.getObisCode());
        readObject(obj, 2);
        return obj;
    }

    /**
     * Legge un profilo generico (Class ID 7) per codice OBIS.
     *
     * @param obisCode codice OBIS del profilo
     * @param from     data di inizio (null per tutti)
     * @param to       data di fine (null per tutti)
     * @return l'oggetto ProfileGeneric con i dati del buffer
     */
    public GXDLMSProfileGeneric readProfileGeneric(String obisCode, Date from, Date to) {
        GXDLMSProfileGeneric obj = new GXDLMSProfileGeneric(obisCode);

        // Leggi capture objects (attributo 3) per conoscere la struttura
        readObject(obj, 3);

        if (from != null && to != null) {
            // Lettura selettiva per range temporale
            try {
                byte[][] readData = gxClient.readRowsByRange(obj, from, to);
                readMultiFrame(readData);
            } catch (Exception e) {
                throw new DlmsCommunicationException(
                        "Errore lettura profilo " + obisCode + " per range temporale", e);
            }
        } else {
            // Leggi tutto il buffer (attributo 2)
            readObject(obj, 2);
        }

        return obj;
    }

    /**
     * Sincronizza l'orologio del contatore con l'ora corrente del server.
     * Usa il metodo UNIX Time (SET 0-0:1.1.0.255) come da UNI/TS 11291-12-2.
     */
    public void syncClock() {
        setUnixTime(Instant.now());
    }

    /**
     * Imposta l'orologio del contatore tramite UNIX Time (double-long-unsigned).
     * Metodo preferito dalla normativa UNI/TS 11291-12-2 per la sincronizzazione.
     *
     * @param timestamp orario da impostare
     */
    public void setUnixTime(Instant timestamp) {
        try {
            GXDLMSData unixTimeObj = new GXDLMSData(CosemObject.UNIX_TIME.getObisCode());
            unixTimeObj.setValue(timestamp.getEpochSecond());

            byte[][] writeData = gxClient.write(unixTimeObj, 2);
            readMultiFrame(writeData);
            log.info("UNIX Time impostato a: {} (epoch: {})", timestamp, timestamp.getEpochSecond());
        } catch (Exception e) {
            throw new DlmsCommunicationException("Errore impostazione UNIX Time", e);
        }
    }

    /**
     * Imposta l'orologio del contatore tramite oggetto Clock (Class 8).
     * Metodo alternativo, utile per impostare data/ora specifica con timezone.
     *
     * @param timestamp orario da impostare
     */
    public void setClock(Instant timestamp) {
        try {
            GXDLMSClock clock = new GXDLMSClock(CosemObject.CLOCK.getObisCode());
            gurux.dlms.GXDateTime gxDateTime = new gurux.dlms.GXDateTime(Date.from(timestamp));
            clock.setTime(gxDateTime);

            byte[][] writeData = gxClient.write(clock, 2);
            readMultiFrame(writeData);
            log.info("Orologio impostato a: {}", timestamp);
        } catch (Exception e) {
            throw new DlmsCommunicationException("Errore impostazione orologio", e);
        }
    }

    /**
     * Esegue uno script del Global Script (Class 9, OBIS 0-0:10.0.0.255).
     * Script rilevanti per UNI/TS 11291-12-2:
     *   1 = NON CONFIGURATO -> NORMALE
     *   4 = MANUTENZIONE -> NORMALE
     *   7 = Reset EOB
     *   8 = Esecuzione forzata EOB
     *  22 = Chiusura esplicita connessione PP4
     *
     * @param scriptId identificativo dello script da eseguire
     */
    public void executeScript(int scriptId) {
        executeMethod(CosemObject.GLOBAL_SCRIPT.getObisCode(), 9, 1, scriptId, DataType.UINT16);
        log.info("Script {} eseguito", scriptId);
    }

    /**
     * Chiude esplicitamente la connessione PP4 (script 22).
     * Da invocare prima di disconnect() per segnalare al contatore
     * che il push e' stato completato con successo (risparmio batteria).
     */
    public void closeSession() {
        try {
            executeScript(22);
        } catch (Exception e) {
            log.debug("Errore chiusura sessione PP4 (ignorato): {}", e.getMessage());
        }
    }

    /**
     * Scrive un attributo di un oggetto COSEM.
     *
     * @param obisCode  codice OBIS dell'oggetto
     * @param classId   Class ID COSEM
     * @param attrIndex indice dell'attributo
     * @param value     valore da scrivere
     * @param dataType  tipo DLMS del valore
     */
    public void writeAttribute(String obisCode, int classId, int attrIndex,
                                Object value, DataType dataType) {
        try {
            GXDLMSObject obj = GXDLMSClient.createObject(ObjectType.forValue(classId));
            obj.setLogicalName(obisCode);

            byte[][] writeData = gxClient.write(obj, attrIndex);
            readMultiFrame(writeData);
            log.info("Attributo {} scritto su {} = {}", attrIndex, obisCode, value);
        } catch (Exception e) {
            throw new DlmsCommunicationException(
                    "Errore scrittura " + obisCode + " attr=" + attrIndex, e);
        }
    }

    /**
     * Invia il comando di disconnessione (chiusura) della valvola gas.
     * DisconnectControl (Class 70), method 1 = remote_disconnect.
     */
    public void disconnectValve() {
        executeMethod(CosemObject.VALVE_STATE.getObisCode(), 70, 1, 0, DataType.INT8);
    }

    /**
     * Invia il comando di riconnessione (apertura) della valvola gas.
     * DisconnectControl (Class 70), method 2 = remote_reconnect.
     */
    public void reconnectValve() {
        executeMethod(CosemObject.VALVE_STATE.getObisCode(), 70, 2, 0, DataType.INT8);
    }

    /**
     * Imposta la password di abilitazione della valvola.
     * OBIS 0-0:94.39.1.255 (Valve Enable Password), tipo long-unsigned.
     *
     * @param password valore numerico della password (0-65535)
     */
    public void setValvePassword(int password) {
        try {
            GXDLMSData obj = new GXDLMSData(CosemObject.VALVE_ENABLE_PASSWORD.getObisCode());
            obj.setValue(password);
            byte[][] writeData = gxClient.write(obj, 2);
            readMultiFrame(writeData);
            log.info("Password valvola impostata");
        } catch (Exception e) {
            throw new DlmsCommunicationException("Errore impostazione password valvola", e);
        }
    }

    /**
     * Imposta la durata di validita' del comando di apertura della valvola (in minuti).
     * OBIS 0-0:94.39.6.255 (Opening Command Duration Validity), tipo long-unsigned.
     *
     * @param durationMinutes durata in minuti (0 = nessun limite)
     */
    public void setValveOpeningDuration(int durationMinutes) {
        try {
            GXDLMSData obj = new GXDLMSData(CosemObject.VALVE_OPENING_DURATION.getObisCode());
            obj.setValue(durationMinutes);
            byte[][] writeData = gxClient.write(obj, 2);
            readMultiFrame(writeData);
            log.info("Durata apertura valvola impostata a {} minuti", durationMinutes);
        } catch (Exception e) {
            throw new DlmsCommunicationException("Errore impostazione durata apertura valvola", e);
        }
    }

    /**
     * Legge lo stato corrente della valvola.
     * Legge output_state (attr 2) e control_state (attr 3) dal DisconnectControl.
     *
     * @return array [output_state (boolean), control_state (int)]
     */
    public Object[] readValveState() {
        String obis = CosemObject.VALVE_STATE.getObisCode();
        GXDLMSDisconnectControl obj = new GXDLMSDisconnectControl(obis);
        readObject(obj, 2); // output_state
        readObject(obj, 3); // control_state
        return new Object[]{ obj.getOutputState(), obj.getControlState() };
    }

    /**
     * Imposta la destinazione push (IP e porta) sul contatore.
     *
     * @param ip   indirizzo IP destinazione (es. "10.0.0.1")
     * @param port porta destinazione (es. 4059)
     */
    public void setPushDestination(String ip, int port) {
        try {
            GXDLMSPushSetup pushSetup = new GXDLMSPushSetup(CosemObject.PUSH_SETUP_1.getObisCode());
            pushSetup.setDestination(ip + ":" + port);

            byte[][] writeData = gxClient.write(pushSetup, 3);
            readMultiFrame(writeData);
            log.info("Destinazione push impostata: {}:{}", ip, port);
        } catch (Exception e) {
            throw new DlmsCommunicationException("Errore impostazione destinazione push", e);
        }
    }

    /**
     * Imposta la destinazione push su uno specifico Push Setup (1-4).
     *
     * @param pushNumber numero del push setup (1-4)
     * @param ip         indirizzo IP destinazione
     * @param port       porta destinazione
     */
    public void setPushDestination(int pushNumber, String ip, int port) {
        String obis = switch (pushNumber) {
            case 1 -> CosemObject.PUSH_SETUP_1.getObisCode();
            case 2 -> CosemObject.PUSH_SETUP_2.getObisCode();
            case 3 -> CosemObject.PUSH_SETUP_3.getObisCode();
            case 4 -> CosemObject.PUSH_SETUP_4.getObisCode();
            default -> throw new DlmsCommunicationException("Push Setup non valido: " + pushNumber);
        };
        try {
            GXDLMSPushSetup pushSetup = new GXDLMSPushSetup(obis);
            pushSetup.setDestination(ip + ":" + port);
            byte[][] writeData = gxClient.write(pushSetup, 3);
            readMultiFrame(writeData);
            log.info("Destinazione Push Setup {} impostata: {}:{}", pushNumber, ip, port);
        } catch (DlmsCommunicationException e) {
            throw e;
        } catch (Exception e) {
            throw new DlmsCommunicationException("Errore impostazione Push Setup " + pushNumber, e);
        }
    }

    /**
     * Legge la compact frame EOB Parameters (CF4).
     *
     * @return dati grezzi della CF4 (byte[])
     */
    public Object readEobParameters() {
        return readData(CosemObject.CF4_EOB_PARAMS.getObisCode());
    }

    /**
     * Legge il piano tariffario attivo (CF5).
     *
     * @return dati grezzi della CF5 (byte[])
     */
    public Object readActiveTariffPlan() {
        return readData(CosemObject.CF5_ACTIVE_TARIFF.getObisCode());
    }

    /**
     * Legge il piano tariffario passivo (CF6).
     *
     * @return dati grezzi della CF6 (byte[])
     */
    public Object readPassiveTariffPlan() {
        return readData(CosemObject.CF6_PASSIVE_TARIFF.getObisCode());
    }

    /**
     * Scrive il piano tariffario passivo (CF6) sul contatore.
     * Il piano verra' attivato alla data di attivazione specificata al suo interno.
     *
     * @param tariffPlanData dati del piano tariffario in formato compact frame
     */
    public void writePassiveTariffPlan(byte[] tariffPlanData) {
        try {
            GXDLMSData obj = new GXDLMSData(CosemObject.CF6_PASSIVE_TARIFF.getObisCode());
            obj.setValue(tariffPlanData);
            byte[][] writeData = gxClient.write(obj, 2);
            readMultiFrame(writeData);
            log.info("Piano tariffario passivo scritto ({} byte)", tariffPlanData.length);
        } catch (Exception e) {
            throw new DlmsCommunicationException("Errore scrittura piano tariffario passivo", e);
        }
    }

    /**
     * Legge la configurazione comunicazione PP4 (CF41).
     *
     * @return dati grezzi della CF41 (byte[])
     */
    public Object readCommSetup() {
        return readData(CosemObject.CF41_COMM_SETUP.getObisCode());
    }

    /**
     * Scrive la configurazione comunicazione PP4 (CF41) sul contatore.
     *
     * @param commSetupData dati configurazione in formato compact frame
     */
    public void writeCommSetup(byte[] commSetupData) {
        try {
            GXDLMSData obj = new GXDLMSData(CosemObject.CF41_COMM_SETUP.getObisCode());
            obj.setValue(commSetupData);
            byte[][] writeData = gxClient.write(obj, 2);
            readMultiFrame(writeData);
            log.info("Configurazione comunicazione PP4 scritta ({} byte)", commSetupData.length);
        } catch (Exception e) {
            throw new DlmsCommunicationException("Errore scrittura configurazione comunicazione PP4", e);
        }
    }

    /**
     * Scrive i parametri EOB (CF4) sul contatore.
     *
     * @param eobData dati EOB in formato compact frame
     */
    public void writeEobParameters(byte[] eobData) {
        try {
            GXDLMSData obj = new GXDLMSData(CosemObject.CF4_EOB_PARAMS.getObisCode());
            obj.setValue(eobData);
            byte[][] writeData = gxClient.write(obj, 2);
            readMultiFrame(writeData);
            log.info("Parametri EOB scritti ({} byte)", eobData.length);
        } catch (Exception e) {
            throw new DlmsCommunicationException("Errore scrittura parametri EOB", e);
        }
    }

    // ========== FIRMWARE UPDATE (Image Transfer, Class 18) ==========

    /**
     * Inizia il trasferimento di un'immagine firmware.
     * Corrisponde al metodo image_transfer_initiate (method 1) della Class 18.
     *
     * @param imageIdentifier identificativo dell'immagine (es. nome file firmware)
     * @param imageSize       dimensione totale in byte dell'immagine
     */
    public void imageTransferInitiate(String imageIdentifier, int imageSize) {
        try {
            GXDLMSImageTransfer img = new GXDLMSImageTransfer(CosemObject.IMAGE_TRANSFER.getObisCode());
            img.setImageTransferEnabled(true);

            // Abilita il trasferimento (attr 5 = true)
            byte[][] enableData = gxClient.write(img, 5);
            readMultiFrame(enableData);

            // Method 1: image_transfer_initiate
            byte[][] actionData = gxClient.method(img, 1,
                    imageIdentifier.getBytes(), DataType.OCTET_STRING);
            readMultiFrame(actionData);
            log.info("Image transfer iniziato: {} ({} byte)", imageIdentifier, imageSize);
        } catch (Exception e) {
            throw new DlmsCommunicationException("Errore inizializzazione image transfer", e);
        }
    }

    /**
     * Trasferisce un blocco di dati dell'immagine firmware.
     * Corrisponde al metodo image_block_transfer (method 2) della Class 18.
     *
     * @param blockNumber numero del blocco (0-based)
     * @param blockData   dati del blocco
     */
    public void imageBlockTransfer(int blockNumber, byte[] blockData) {
        try {
            GXDLMSImageTransfer img = new GXDLMSImageTransfer(CosemObject.IMAGE_TRANSFER.getObisCode());

            // Method 2: image_block_transfer - invia il blocco come byte array
            byte[][] actionData = gxClient.method(img, 2, blockData, DataType.OCTET_STRING);
            readMultiFrame(actionData);
            log.debug("Image block {} trasferito ({} byte)", blockNumber, blockData.length);
        } catch (Exception e) {
            throw new DlmsCommunicationException("Errore trasferimento blocco " + blockNumber, e);
        }
    }

    /**
     * Verifica l'immagine firmware trasferita.
     * Corrisponde al metodo image_verify (method 3) della Class 18.
     */
    public void imageVerify() {
        try {
            GXDLMSImageTransfer img = new GXDLMSImageTransfer(CosemObject.IMAGE_TRANSFER.getObisCode());
            byte[][] actionData = gxClient.method(img, 3, 0, DataType.INT8);
            readMultiFrame(actionData);
            log.info("Image verify eseguito");
        } catch (Exception e) {
            throw new DlmsCommunicationException("Errore verifica immagine firmware", e);
        }
    }

    /**
     * Attiva l'immagine firmware verificata.
     * Corrisponde al metodo image_activate (method 4) della Class 18.
     * Dopo l'attivazione il contatore si riavviera' con il nuovo firmware.
     */
    public void imageActivate() {
        try {
            GXDLMSImageTransfer img = new GXDLMSImageTransfer(CosemObject.IMAGE_TRANSFER.getObisCode());
            byte[][] actionData = gxClient.method(img, 4, 0, DataType.INT8);
            readMultiFrame(actionData);
            log.info("Image activate eseguito - il contatore si riavviera'");
        } catch (Exception e) {
            throw new DlmsCommunicationException("Errore attivazione immagine firmware", e);
        }
    }

    /**
     * Legge lo stato del trasferimento firmware (CF22).
     *
     * @return dati grezzi della CF22 (byte[])
     */
    public Object readFirmwareTransferStatus() {
        return readData(CosemObject.CF22_FW_STATUS.getObisCode());
    }

    // ========== CAMBIO CHIAVI CRITTOGRAFICHE ==========

    /**
     * Cambia la chiave HLS (High Level Security secret) di un'associazione.
     * Corrisponde al metodo change_HLS_secret (method 2) della Class 15 (AssociationLN).
     *
     * Associazioni disponibili:
     * - Management:     0-0:40.0.1.255
     * - I/M:            0-0:40.0.3.255
     * - GA:             0-0:40.0.48.255
     * - Broadcasting:   0-0:40.0.32.255
     *
     * @param associationObis OBIS dell'associazione da aggiornare
     * @param newSecret       nuova chiave/secret (16 byte per AES-128)
     */
    public void changeHlsSecret(String associationObis, byte[] newSecret) {
        try {
            GXDLMSAssociationLogicalName assoc = new GXDLMSAssociationLogicalName(associationObis);
            byte[][] actionData = gxClient.method(assoc, 2, newSecret, DataType.OCTET_STRING);
            readMultiFrame(actionData);
            log.info("Chiave HLS aggiornata per associazione {}", associationObis);
        } catch (Exception e) {
            throw new DlmsCommunicationException(
                    "Errore cambio chiave HLS per " + associationObis, e);
        }
    }

    /**
     * Aggiorna la Global Encryption Key (chiave di cifratura) tramite SecuritySetup.
     * Corrisponde al metodo global_key_transfer (method 2) della Class 64.
     *
     * @param securitySetupObis OBIS del SecuritySetup (es. 0-0:43.0.1.255 per Management)
     * @param wrappedKey        nuova chiave avvolta (key-wrapped) con la master key
     */
    public void globalKeyTransfer(String securitySetupObis, byte[] wrappedKey) {
        try {
            GXDLMSSecuritySetup secSetup = new GXDLMSSecuritySetup(securitySetupObis);
            byte[][] actionData = gxClient.method(secSetup, 2, wrappedKey, DataType.ARRAY);
            readMultiFrame(actionData);
            log.info("Global key transfer eseguito per {}", securitySetupObis);
        } catch (Exception e) {
            throw new DlmsCommunicationException(
                    "Errore global key transfer per " + securitySetupObis, e);
        }
    }

    // ========== GESTIONE INSTALLER/MAINTAINER ==========

    /**
     * Abilita o disabilita l'associazione Installer/Maintainer.
     * Scrive il campo "permission" dell'oggetto I/M Setup (0-0:94.39.30.255).
     *
     * @param permissionBitmask bitmask dei diritti (vedi Appendice H della 11291-12-2)
     *                          B0=modifiche, B1=reset eventi, B2=parametri metrologia,
     *                          B3=valvola, B4=batteria, B5=clock, B6=DB reset, B7=PP4
     */
    public void setImPermissions(int permissionBitmask) {
        try {
            GXDLMSData obj = new GXDLMSData(CosemObject.IM_SETUP.getObisCode());
            obj.setValue(permissionBitmask);
            byte[][] writeData = gxClient.write(obj, 2);
            readMultiFrame(writeData);
            log.info("Permessi I/M impostati: 0x{}", Integer.toHexString(permissionBitmask));
        } catch (Exception e) {
            throw new DlmsCommunicationException("Errore impostazione permessi I/M", e);
        }
    }

    /**
     * Legge il tempo residuo della sessione Installer/Maintainer.
     * OBIS 0-0:94.39.31.255 (I/M Remaining Time), valore in secondi.
     *
     * @return secondi residui, oppure -1 se non disponibile
     */
    public long readImRemainingTime() {
        try {
            Object value = readData(CosemObject.IM_REMAINING_TIME.getObisCode());
            if (value instanceof Number n) return n.longValue();
            return -1;
        } catch (Exception e) {
            log.debug("Impossibile leggere tempo residuo I/M: {}", e.getMessage());
            return -1;
        }
    }

    /**
     * Verifica se la connessione DLMS e' attiva.
     */
    public boolean isConnected() {
        return connected && transport.isConnected();
    }

    /**
     * Restituisce il GXDLMSClient sottostante (per operazioni avanzate).
     */
    public GXDLMSClient getGxClient() {
        return gxClient;
    }

    // --- Metodi privati ---

    /**
     * Legge un singolo attributo di un oggetto COSEM.
     */
    private void readObject(GXDLMSObject obj, int attributeIndex) {
        try {
            byte[][] readData = gxClient.read(obj, attributeIndex);
            readMultiFrame(readData);
        } catch (Exception e) {
            throw new DlmsCommunicationException(
                    "Errore lettura " + obj.getLogicalName() + " attr=" + attributeIndex, e);
        }
    }

    /**
     * Gestisce la lettura multi-frame (block transfer).
     * Invia ogni frame e gestisce eventuali blocchi aggiuntivi.
     */
    private void readMultiFrame(byte[][] frames) {
        try {
            GXReplyData reply = new GXReplyData();
            for (byte[] frame : frames) {
                reply.clear();
                sendReceive(frame, reply);

                // Gestisci block transfer: continua a richiedere blocchi
                while (reply.isMoreData()) {
                    byte[] moreFrame = gxClient.receiverReady(reply);
                    sendReceive(moreFrame, reply);
                }
            }
        } catch (DlmsCommunicationException e) {
            throw e;
        } catch (Exception e) {
            throw new DlmsCommunicationException("Errore durante lettura multi-frame", e);
        }
    }

    /**
     * Invoca un metodo (ACTION) su un oggetto COSEM.
     */
    private void executeMethod(String obisCode, int classId, int methodIndex,
                                Object data, DataType dataType) {
        try {
            GXDLMSObject obj = gxClient.getObjects().findByLN(
                    ObjectType.forValue(classId), obisCode);
            if (obj == null) {
                obj = GXDLMSClient.createObject(ObjectType.forValue(classId));
                obj.setLogicalName(obisCode);
            }

            byte[][] actionData = gxClient.method(obj, methodIndex, data, dataType);
            readMultiFrame(actionData);
            log.info("Metodo {} eseguito su {}", methodIndex, obisCode);
        } catch (Exception e) {
            throw new DlmsCommunicationException(
                    "Errore esecuzione metodo " + methodIndex + " su " + obisCode, e);
        }
    }

    /**
     * Invia un frame e riceve la risposta, aggiornando il GXReplyData.
     */
    private void sendReceive(byte[] frame, GXReplyData reply) {
        try {
            transport.send(frame);
            boolean complete = false;
            while (!complete) {
                byte[] received = transport.receive();
                complete = gxClient.getData(received, reply);
            }
        } catch (IOException e) {
            throw new DlmsCommunicationException("Errore I/O durante comunicazione DLMS", e);
        } catch (Exception e) {
            throw new DlmsCommunicationException("Errore durante parsing risposta DLMS", e);
        }
    }

    /**
     * Mappa la stringa di autenticazione al tipo gurux.
     */
    private static Authentication mapAuthentication(String auth) {
        if (auth == null || auth.isEmpty()) {
            return Authentication.NONE;
        }
        return switch (auth.toUpperCase()) {
            case "NONE" -> Authentication.NONE;
            case "LOW" -> Authentication.LOW;
            case "HIGH" -> Authentication.HIGH;
            case "HIGH_MD5" -> Authentication.HIGH_MD5;
            case "HIGH_SHA1" -> Authentication.HIGH_SHA1;
            case "HIGH_SHA256" -> Authentication.HIGH_SHA256;
            case "HIGH_GMAC" -> Authentication.HIGH_GMAC;
            default -> throw new IllegalArgumentException("Livello di autenticazione non supportato: " + auth);
        };
    }
}
