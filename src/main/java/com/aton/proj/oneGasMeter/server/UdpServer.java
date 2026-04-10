package com.aton.proj.oneGasMeter.server;

import com.aton.proj.oneGasMeter.config.UdpServerConfig;
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

    private final UdpServerConfig config;
    private DatagramSocket socket;
    private ExecutorService executor;
    private volatile boolean running = false;

    public UdpServer(UdpServerConfig config) {
        this.config = config;
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
     * Per ora logga il contenuto. Sara' esteso per decodificare il WRAPPER
     * e processare DATA-NOTIFICATION o rispondere a GET/SET.
     */
    private void handlePacket(byte[] data, InetAddress senderAddress, int senderPort) {
        String senderIp = senderAddress.getHostAddress();
        try {
            // TODO: Decodifica DLMS WRAPPER header e processa APDU
            log.info("Elaborazione pacchetto UDP da {}:{} ({} byte)", senderIp, senderPort, data.length);

            // Il processing completo sara' implementato nelle fasi successive

        } catch (Exception e) {
            log.error("Errore elaborazione pacchetto UDP da {}:{}: {}", senderIp, senderPort, e.getMessage(), e);
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
