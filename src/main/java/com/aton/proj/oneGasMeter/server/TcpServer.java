package com.aton.proj.oneGasMeter.server;

import com.aton.proj.oneGasMeter.config.DlmsSessionConfig;
import com.aton.proj.oneGasMeter.config.TcpServerConfig;
import com.aton.proj.oneGasMeter.service.CommandService;
import com.aton.proj.oneGasMeter.service.TelemetryService;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

/**
 * Server TCP che accetta connessioni in ingresso dai contatori gas.
 * Ogni connessione viene gestita in un virtual thread dedicato.
 * Il numero di connessioni contemporanee e' limitato da un semaforo.
 */
@Component
public class TcpServer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(TcpServer.class);

    private final TcpServerConfig tcpConfig;
    private final DlmsSessionConfig dlmsConfig;
    private final TelemetryService telemetryService;
    private final CommandService commandService;

    private ServerSocket serverSocket;
    private ExecutorService executor;
    private volatile boolean running = false;

    public TcpServer(TcpServerConfig tcpConfig, DlmsSessionConfig dlmsConfig,
                     TelemetryService telemetryService, CommandService commandService) {
        this.tcpConfig = tcpConfig;
        this.dlmsConfig = dlmsConfig;
        this.telemetryService = telemetryService;
        this.commandService = commandService;
    }

    @Override
    public void run(String... args) throws Exception {
        // Se la porta e' 0, il server non si avvia (utile per i test)
        if (tcpConfig.getPort() == 0) {
            log.info("Server TCP disabilitato (porta=0)");
            return;
        }

        serverSocket = new ServerSocket(tcpConfig.getPort(), tcpConfig.getBacklog());
        executor = Executors.newVirtualThreadPerTaskExecutor();
        Semaphore semaphore = new Semaphore(tcpConfig.getMaxConnections());
        running = true;

        log.info("Server TCP in ascolto sulla porta {} (max connessioni: {}, backlog: {})",
                tcpConfig.getPort(), tcpConfig.getMaxConnections(), tcpConfig.getBacklog());

        // Loop di accettazione connessioni
        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                String clientIp = clientSocket.getInetAddress().getHostAddress();
                log.info("Nuova connessione da {}", clientIp);

                executor.submit(() -> {
                    if (!semaphore.tryAcquire()) {
                        log.warn("Connessione rifiutata da {} - limite raggiunto", clientIp);
                        closeQuietly(clientSocket);
                        return;
                    }
                    try {
                        MeterSessionHandler handler = new MeterSessionHandler(
                                clientSocket, dlmsConfig, tcpConfig.getSessionTimeoutMs(),
                                telemetryService, commandService);
                        handler.handle();
                    } finally {
                        semaphore.release();
                    }
                });
            } catch (SocketException e) {
                if (running) {
                    log.error("Errore nel server socket: {}", e.getMessage());
                }
                // Se running=false, il server e' stato chiuso intenzionalmente
            } catch (IOException e) {
                log.error("Errore durante accettazione connessione", e);
            }
        }
    }

    @PreDestroy
    public void shutdown() {
        running = false;
        log.info("Arresto server TCP...");

        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                log.warn("Errore chiusura server socket: {}", e.getMessage());
            }
        }
        if (executor != null) {
            executor.close();
        }
        log.info("Server TCP arrestato");
    }

    /**
     * Indica se il server e' in ascolto.
     */
    public boolean isRunning() {
        return running;
    }

    private void closeQuietly(Socket socket) {
        try {
            socket.close();
        } catch (IOException e) {
            // ignora
        }
    }
}
