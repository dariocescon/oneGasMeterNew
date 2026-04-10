package com.aton.proj.oneGasMeter.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Entita' JPA per i dati di telemetria ricevuti dai contatori gas.
 * Ogni riga rappresenta una singola lettura di un oggetto COSEM.
 */
@Entity
@Table(name = "telemetry_data", indexes = {
    @Index(name = "ix_telemetry_serial", columnList = "serialNumber"),
    @Index(name = "ix_telemetry_session", columnList = "sessionId"),
    @Index(name = "ix_telemetry_received", columnList = "receivedAt")
})
public class TelemetryData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Numero seriale del contatore */
    @Column(nullable = false, length = 64)
    private String serialNumber;

    /** Indirizzo IP del contatore */
    @Column(nullable = false, length = 45)
    private String meterIp;

    /** Codice OBIS dell'oggetto COSEM letto */
    @Column(nullable = false, length = 30)
    private String obisCode;

    /** Class ID COSEM */
    @Column(nullable = false)
    private int classId;

    /** Valore grezzo come stringa */
    @Column(columnDefinition = "TEXT")
    private String rawValue;

    /** Scalatore dal registro COSEM */
    @Column(nullable = false)
    private double scaler = 1.0;

    /** Unita' di misura */
    @Column(length = 20)
    private String unit;

    /** Valore scalato (rawValue * 10^scaler) */
    private Double scaledValue;

    /** Timestamp letto dall'orologio del contatore */
    private Instant meterTimestamp;

    /** Timestamp di ricezione sul server */
    @Column(nullable = false)
    private Instant receivedAt;

    /** UUID della sessione di comunicazione */
    @Column(nullable = false, length = 36)
    private String sessionId;

    public TelemetryData() {
    }

    // --- Getters e Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }

    public String getMeterIp() { return meterIp; }
    public void setMeterIp(String meterIp) { this.meterIp = meterIp; }

    public String getObisCode() { return obisCode; }
    public void setObisCode(String obisCode) { this.obisCode = obisCode; }

    public int getClassId() { return classId; }
    public void setClassId(int classId) { this.classId = classId; }

    public String getRawValue() { return rawValue; }
    public void setRawValue(String rawValue) { this.rawValue = rawValue; }

    public double getScaler() { return scaler; }
    public void setScaler(double scaler) { this.scaler = scaler; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public Double getScaledValue() { return scaledValue; }
    public void setScaledValue(Double scaledValue) { this.scaledValue = scaledValue; }

    public Instant getMeterTimestamp() { return meterTimestamp; }
    public void setMeterTimestamp(Instant meterTimestamp) { this.meterTimestamp = meterTimestamp; }

    public Instant getReceivedAt() { return receivedAt; }
    public void setReceivedAt(Instant receivedAt) { this.receivedAt = receivedAt; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    @Override
    public String toString() {
        return "TelemetryData{serialNumber='" + serialNumber + "', obisCode='" + obisCode +
               "', rawValue='" + rawValue + "', scaledValue=" + scaledValue + "}";
    }
}
