package com.aton.proj.oneGasMeter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configurazione del server TCP.
 * Valori letti da application.properties con prefisso "tcp.server".
 */
@Component
@ConfigurationProperties(prefix = "tcp.server")
public class TcpServerConfig {

    /** Porta TCP di ascolto */
    private int port = 60103;

    /** Numero massimo di connessioni contemporanee */
    private int maxConnections = 10000;

    /** Dimensione della coda di backlog del ServerSocket */
    private int backlog = 1000;

    /** Timeout per sessione in millisecondi */
    private int sessionTimeoutMs = 30000;

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    public int getMaxConnections() { return maxConnections; }
    public void setMaxConnections(int maxConnections) { this.maxConnections = maxConnections; }

    public int getBacklog() { return backlog; }
    public void setBacklog(int backlog) { this.backlog = backlog; }

    public int getSessionTimeoutMs() { return sessionTimeoutMs; }
    public void setSessionTimeoutMs(int sessionTimeoutMs) { this.sessionTimeoutMs = sessionTimeoutMs; }
}
