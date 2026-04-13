package com.aton.proj.oneGasMeter.server;

import com.aton.proj.oneGasMeter.config.UdpServerConfig;
import com.aton.proj.oneGasMeter.cosem.CompactFrameData;
import com.aton.proj.oneGasMeter.cosem.CompactFrameParser;
import com.aton.proj.oneGasMeter.service.TelemetryService;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Server UDP che riceve pacchetti DLMS/COSEM dai contatori gas.
 *
 * I contatori gas possono inviare DATA-NOTIFICATION via UDP (connection-less).
 * In caso di fallimento nell'invio della risposta, il server ritenta
 * fino a retryCount volte con retryDelayMs di attesa tra i tentativi.
 *
 * Il protocollo DLMS WRAPPER e' lo stesso usato su TCP: header di 8 byte
 * (version, source wPort, dest wPort, length) seguito dal payload APDU.
 */
@Component
@Order(2)
public class UdpServer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(UdpServer.class);

    private static final int WRAPPER_HEADER_SIZE = 8;
    private static final int WRAPPER_VERSION = 0x0001;
    private static final int DATA_NOTIFICATION_TAG = 0x0F;

    private final UdpServerConfig config;
    private final TelemetryService telemetryService;
    private DatagramSocket socket;
    private ExecutorService executor;
    private volatile boolean running = false;

    public UdpServer(UdpServerConfig config, TelemetryService telemetryService) {
        this.config = config;
        this.telemetryService = telemetryService;
    }

    @Override
    public void run(String... args) throws Exception {
        if (config.getPort() == 0) {
            log.info("Server UDP disabilitato (porta=0)");
            return;
        }

        socket = new DatagramSocket(config.getPort());
        executor = Executors.newVirtualThreadPerTaskExecutor();
        running = true;

        log.info("Server UDP in ascolto sulla porta {} (max pacchetto: {} byte, retry: {}x{}ms)",
                config.getPort(), config.getMaxPacketSize(), config.getRetryCount(), config.getRetryDelayMs());

        // Loop di ricezione pacchetti
        while (running) {
            try {
                byte[] buffer = new byte[config.getMaxPacketSize()];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                // Copia i dati ricevuti (il buffer verra' riusato)
                byte[] data = new byte[packet.getLength()];
                System.arraycopy(packet.getData(), packet.getOffset(), data, 0, packet.getLength());
                InetAddress senderAddress = packet.getAddress();
                int senderPort = packet.getPort();

                log.info("Pacchetto UDP ricevuto da {}:{} ({} byte)",
                        senderAddress.getHostAddress(), senderPort, data.length);

                executor.submit(() -> handlePacket(data, senderAddress, senderPort));

            } catch (IOException e) {
                if (running) {
                    log.error("Errore ricezione pacchetto UDP: {}", e.getMessage());
                }
            }
        }
    }

    /**
     * Gestisce un singolo pacchetto UDP ricevuto.
     * Valida l'header DLMS WRAPPER, individua il tipo APDU e delega
     * l'elaborazione al metodo appropriato.
     */
    private void handlePacket(byte[] data, InetAddress senderAddress, int senderPort) {
        String senderIp = senderAddress.getHostAddress();
        try {
            if (data.length < WRAPPER_HEADER_SIZE) {
                log.warn("Pacchetto UDP troppo corto da {}:{}: {} byte", senderIp, senderPort, data.length);
                return;
            }

            // Valida versione WRAPPER
            int version = ((data[0] & 0xFF) << 8) | (data[1] & 0xFF);
            if (version != WRAPPER_VERSION) {
                log.warn("Versione WRAPPER non valida da {}:{}: 0x{}", senderIp, senderPort,
                        Integer.toHexString(version));
                return;
            }

            // Estrai lunghezza payload dai byte 6-7
            int payloadLength = ((data[6] & 0xFF) << 8) | (data[7] & 0xFF);
            if (data.length < WRAPPER_HEADER_SIZE + payloadLength) {
                log.warn("Pacchetto UDP incompleto da {}:{}: attesi {} byte, ricevuti {}",
                        senderIp, senderPort, WRAPPER_HEADER_SIZE + payloadLength, data.length);
                return;
            }

            // Estrai APDU (payload)
            byte[] apdu = Arrays.copyOfRange(data, WRAPPER_HEADER_SIZE, WRAPPER_HEADER_SIZE + payloadLength);

            int tag = apdu[0] & 0xFF;
            if (tag == DATA_NOTIFICATION_TAG) {
                processDataNotification(apdu, senderIp);
            } else {
                log.debug("Tag APDU non gestito da {}:{}: 0x{}", senderIp, senderPort,
                        Integer.toHexString(tag));
            }

        } catch (Exception e) {
            log.error("Errore elaborazione pacchetto UDP da {}:{}: {}", senderIp, senderPort, e.getMessage(), e);
        }
    }

    /**
     * Elabora un APDU DATA-NOTIFICATION (tag 0x0F).
     *
     * Struttura DATA-NOTIFICATION:
     *   [0]    tag = 0x0F
     *   [1-4]  long-invoke-id-and-priority
     *   [5]    lunghezza date-time (0 se assente)
     *   [5+1..5+1+len-1] date-time (se presente)
     *   [...]  notification-body (Data)
     */
    private void processDataNotification(byte[] apdu, String senderIp) {
        if (apdu.length < 6) {
            log.warn("DATA-NOTIFICATION troppo corta da {}: {} byte", senderIp, apdu.length);
            return;
        }

        // Salta tag (1) + invoke-id-and-priority (4)
        int offset = 5;

        // Salta date-time (octet-string con lunghezza prefissata)
        int dtLength = apdu[offset] & 0xFF;
        offset += 1 + dtLength;

        if (offset >= apdu.length) {
            log.debug("DATA-NOTIFICATION senza corpo da {}", senderIp);
            return;
        }

        // Il resto e' il notification-body (Data): cerca il compact frame buffer
        byte[] compactBuffer = findOctetString(apdu, offset);
        if (compactBuffer == null) {
            log.debug("Nessun compact frame buffer nel pacchetto UDP da {}", senderIp);
            return;
        }

        CompactFrameData cfData = CompactFrameParser.parse(compactBuffer);
        if (cfData == null) {
            log.debug("Compact frame non parsabile da {}", senderIp);
            return;
        }

        String sessionId = UUID.randomUUID().toString();
        saveCompactFrameData(cfData, senderIp, senderIp, sessionId);
        log.info("DATA-NOTIFICATION UDP da {}: CF{} elaborata ({} campi)",
                senderIp, cfData.getTemplateId(), cfData.getValues().size());
    }

    /**
     * Naviga ricorsivamente la struttura DLMS Data cercando il primo octet-string
     * il cui primo byte corrisponde a un template ID di compact frame supportato.
     *
     * Tipi DLMS gestiti:
     *   0x01 = array, 0x02 = structure, 0x09 = octet-string
     *
     * @param apdu   buffer contenente l'APDU o una sua sottosequenza
     * @param offset posizione di partenza della ricerca
     * @return byte[] del compact frame buffer, oppure null se non trovato
     */
    private byte[] findOctetString(byte[] apdu, int offset) {
        if (offset >= apdu.length) {
            return null;
        }

        int tag = apdu[offset] & 0xFF;
        offset++;

        switch (tag) {
            case 0x09: { // octet-string: lunghezza (1 byte) + dati
                if (offset >= apdu.length) return null;
                int len = apdu[offset] & 0xFF;
                offset++;
                if (len >= 2 && offset + len <= apdu.length) {
                    int templateId = apdu[offset] & 0xFF;
                    if (isKnownTemplate(templateId)) {
                        return Arrays.copyOfRange(apdu, offset, offset + len);
                    }
                }
                return null;
            }
            case 0x02: { // structure: contatore (1 byte) + elementi
                if (offset >= apdu.length) return null;
                int count = apdu[offset] & 0xFF;
                int pos = offset + 1;
                for (int i = 0; i < count; i++) {
                    byte[] result = findOctetString(apdu, pos);
                    if (result != null) return result;
                    int next = skipDataElement(apdu, pos);
                    if (next <= pos || next >= apdu.length) break; // sicurezza: nessun progresso o fine buffer
                    pos = next;
                }
                return null;
            }
            case 0x01: { // array: contatore (2 byte) + elementi
                if (offset + 1 >= apdu.length) return null;
                int count = ((apdu[offset] & 0xFF) << 8) | (apdu[offset + 1] & 0xFF);
                int pos = offset + 2;
                for (int i = 0; i < count; i++) {
                    byte[] result = findOctetString(apdu, pos);
                    if (result != null) return result;
                    int next = skipDataElement(apdu, pos);
                    if (next <= pos || next >= apdu.length) break;
                    pos = next;
                }
                return null;
            }
            default:
                return null;
        }
    }

    /**
     * Calcola la posizione del prossimo elemento DLMS Data dopo quello
     * che inizia a {@code offset}.
     *
     * @param apdu   buffer APDU
     * @param offset offset del tag dell'elemento corrente
     * @return offset del byte immediatamente successivo all'elemento
     */
    private int skipDataElement(byte[] apdu, int offset) {
        if (offset >= apdu.length) return offset;

        int tag = apdu[offset] & 0xFF;
        offset++;

        switch (tag) {
            case 0x00: return offset;               // null-data
            case 0x03: return offset + 1;           // boolean
            case 0x05: return offset + 4;           // int32
            case 0x06: return offset + 4;           // unsigned int32
            case 0x07: return offset + 8;           // int64
            case 0x08: return offset + 8;           // unsigned int64
            case 0x09:                              // octet-string
            case 0x0A:                              // visible-string
            case 0x0C: {                            // utf8-string
                if (offset >= apdu.length) return offset;
                int len = apdu[offset] & 0xFF;
                return offset + 1 + len;
            }
            case 0x0F: return offset + 1;           // bcd
            case 0x10: return offset + 1;           // integer (int8)
            case 0x11: return offset + 2;           // long (int16)
            case 0x12: return offset + 1;           // unsigned integer (uint8)
            case 0x13: return offset + 2;           // long-unsigned (uint16)
            case 0x15: return offset + 8;           // long64
            case 0x16: return offset + 8;           // unsigned long64
            case 0x17: return offset + 1;           // enum
            case 0x18: return offset + 4;           // float32
            case 0x19: return offset + 8;           // float64
            case 0x1A: return offset + 12;          // date-time (12 byte)
            case 0x1B: return offset + 5;           // date (5 byte)
            case 0x1C: return offset + 4;           // time (4 byte)
            case 0x1E: return offset;               // dont-care
            case 0x02: {                            // structure
                if (offset >= apdu.length) return offset;
                int count = apdu[offset] & 0xFF;
                int pos = offset + 1;
                for (int i = 0; i < count; i++) {
                    int next = skipDataElement(apdu, pos);
                    if (next <= pos || next >= apdu.length) return apdu.length;
                    pos = next;
                }
                return pos;
            }
            case 0x01: {                            // array
                if (offset + 1 >= apdu.length) return offset;
                int count = ((apdu[offset] & 0xFF) << 8) | (apdu[offset + 1] & 0xFF);
                int pos = offset + 2;
                for (int i = 0; i < count; i++) {
                    int next = skipDataElement(apdu, pos);
                    if (next <= pos || next >= apdu.length) return apdu.length;
                    pos = next;
                }
                return pos;
            }
            default:
                return apdu.length; // tag sconosciuto: segnala impossibilita' di avanzare
        }
    }

    /**
     * Verifica se il template ID corrisponde a un tipo di compact frame supportato.
     */
    private boolean isKnownTemplate(int templateId) {
        return switch (templateId) {
            case 3, 4, 5, 6, 7, 8, 9, 22, 41, 47, 48, 49, 51 -> true;
            default -> false;
        };
    }

    /**
     * Salva tutti i valori scalari di una compact frame parsata in telemetry_data.
     * I valori binari (profili, snapshot) vengono omessi perche' troppo grandi.
     */
    private void saveCompactFrameData(CompactFrameData cfData, String serialNumber,
                                       String meterIp, String sessionId) {
        Instant timestamp = cfData.getTimestamp() != null ? cfData.getTimestamp() : Instant.now();

        for (var entry : cfData.getValues().entrySet()) {
            String obisCode = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof byte[]) continue;

            telemetryService.save(serialNumber, meterIp, sessionId,
                    obisCode, 62, value, 0, null, timestamp);
        }
    }

    /**
     * Invia una risposta UDP con meccanismo di retry.
     *
     * @param data           dati da inviare
     * @param targetAddress  indirizzo destinatario
     * @param targetPort     porta destinatario
     * @return true se l'invio ha avuto successo
     */
    public boolean sendWithRetry(byte[] data, InetAddress targetAddress, int targetPort) {
        for (int attempt = 1; attempt <= config.getRetryCount(); attempt++) {
            try {
                DatagramPacket response = new DatagramPacket(data, data.length, targetAddress, targetPort);
                socket.send(response);
                log.debug("Risposta UDP inviata a {}:{} (tentativo {})", targetAddress.getHostAddress(), targetPort, attempt);
                return true;
            } catch (IOException e) {
                log.warn("Invio risposta UDP fallito (tentativo {}/{}): {}", attempt, config.getRetryCount(), e.getMessage());
                if (attempt < config.getRetryCount()) {
                    try {
                        Thread.sleep(config.getRetryDelayMs());
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return false;
                    }
                }
            }
        }
        log.error("Invio risposta UDP fallito dopo {} tentativi verso {}:{}",
                config.getRetryCount(), targetAddress.getHostAddress(), targetPort);
        return false;
    }

    @PreDestroy
    public void shutdown() {
        running = false;
        log.info("Arresto server UDP...");

        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        if (executor != null) {
            executor.close();
        }
        log.info("Server UDP arrestato");
    }

    public boolean isRunning() {
        return running;
    }
}
