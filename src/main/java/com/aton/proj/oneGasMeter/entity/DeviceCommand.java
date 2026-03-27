package com.aton.proj.oneGasMeter.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Entita' JPA per i comandi da inviare ai contatori gas.
 * I comandi vengono inseriti con stato PENDING e aggiornati durante la sessione.
 */
@Entity
@Table(name = "device_commands", indexes = {
    @Index(name = "ix_commands_serial_status", columnList = "serialNumber, status")
})
public class DeviceCommand {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Numero seriale del contatore destinatario */
    @Column(nullable = false, length = 64)
    private String serialNumber;

    /** Tipo di comando */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private CommandType commandType;

    /** Parametri del comando in formato JSON */
    @Column(columnDefinition = "TEXT")
    private String payload;

    /** Stato corrente del comando */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CommandStatus status = CommandStatus.PENDING;

    /** Data/ora di creazione del comando */
    @Column(nullable = false)
    private Instant createdAt;

    /** Data/ora di esecuzione del comando */
    private Instant executedAt;

    /** Messaggio di errore in caso di fallimento */
    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    public DeviceCommand() {
    }

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    // --- Getters e Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }

    public CommandType getCommandType() { return commandType; }
    public void setCommandType(CommandType commandType) { this.commandType = commandType; }

    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }

    public CommandStatus getStatus() { return status; }
    public void setStatus(CommandStatus status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getExecutedAt() { return executedAt; }
    public void setExecutedAt(Instant executedAt) { this.executedAt = executedAt; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    @Override
    public String toString() {
        return "DeviceCommand{serialNumber='" + serialNumber + "', commandType=" + commandType +
               ", status=" + status + "}";
    }
}
