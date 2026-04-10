package com.aton.proj.oneGasMeter.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Log delle sessioni di comunicazione con i contatori gas.
 * Traccia ogni connessione: protocollo (TCP/UDP), durata, esito.
 */
@Entity
@Table(name = "session_log", indexes = {
    @Index(name = "ix_session_serial", columnList = "serialNumber"),
    @Index(name = "ix_session_started", columnList = "startedAt")
})
public class SessionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** UUID della sessione */
    @Column(nullable = false, length = 36)
    private String sessionId;

    /** Numero seriale del contatore (null se non ancora identificato) */
    @Column(length = 64)
    private String serialNumber;

    /** Indirizzo IP del contatore */
    @Column(nullable = false, length = 45)
    private String meterIp;

    /** Protocollo di comunicazione: TCP o UDP */
    @Column(nullable = false, length = 3)
    private String protocol;

    /** Inizio sessione */
    @Column(nullable = false)
    private Instant startedAt;

    /** Fine sessione */
    private Instant endedAt;

    /** Stato: STARTED, COMPLETED, FAILED */
    @Column(nullable = false, length = 20)
    private String status = "STARTED";

    /** Messaggio di errore se fallita */
    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    /** Numero di oggetti COSEM letti con successo */
    private int objectsRead = 0;

    /** Numero di comandi eseguiti */
    private int commandsExecuted = 0;

    public SessionLog() {
    }

    @PrePersist
    void prePersist() {
        if (startedAt == null) startedAt = Instant.now();
    }

    // --- Getters e Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }

    public String getMeterIp() { return meterIp; }
    public void setMeterIp(String meterIp) { this.meterIp = meterIp; }

    public String getProtocol() { return protocol; }
    public void setProtocol(String protocol) { this.protocol = protocol; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public Instant getEndedAt() { return endedAt; }
    public void setEndedAt(Instant endedAt) { this.endedAt = endedAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public int getObjectsRead() { return objectsRead; }
    public void setObjectsRead(int objectsRead) { this.objectsRead = objectsRead; }

    public int getCommandsExecuted() { return commandsExecuted; }
    public void setCommandsExecuted(int commandsExecuted) { this.commandsExecuted = commandsExecuted; }
}
