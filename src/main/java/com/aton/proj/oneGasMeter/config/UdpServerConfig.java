package com.aton.proj.oneGasMeter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configurazione del server UDP.
 * Valori letti da application.properties con prefisso "udp.server".
 */
@Component
@ConfigurationProperties(prefix = "udp.server")
public class UdpServerConfig {

    /** Porta UDP di ascolto */
    private int port = 60104;

    /** Dimensione massima del pacchetto UDP in byte */
    private int maxPacketSize = 2048;

    /** Numero di tentativi in caso di fallimento invio risposta */
    private int retryCount = 3;

    /** Ritardo tra i tentativi in millisecondi */
    private int retryDelayMs = 1000;

    /**
     * Timeout in millisecondi per le sessioni GBT stantie.
     * Se un contatore si interrompe nel mezzo di un trasferimento GBT,
     * il contesto di riassemblaggio viene eliminato dopo questo intervallo.
     */
    private int sessionTimeoutMs = 30000;

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    public int getMaxPacketSize() { return maxPacketSize; }
    public void setMaxPacketSize(int maxPacketSize) { this.maxPacketSize = maxPacketSize; }

    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }

    public int getRetryDelayMs() { return retryDelayMs; }
    public void setRetryDelayMs(int retryDelayMs) { this.retryDelayMs = retryDelayMs; }

    public int getSessionTimeoutMs() { return sessionTimeoutMs; }
    public void setSessionTimeoutMs(int sessionTimeoutMs) { this.sessionTimeoutMs = sessionTimeoutMs; }
}
