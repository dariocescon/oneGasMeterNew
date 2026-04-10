package com.aton.proj.oneGasMeter.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Anagrafica dispositivi con chiavi crittografiche DLMS.
 *
 * Le chiavi (encryption_key, authentication_key, master_key) sono salvate
 * cifrate con AES-256-GCM. Usare KeyEncryptionService per cifrarle/decifrarle.
 *
 * Campi chiave secondo UNI/TS 11291-10:
 * - encryption_key: chiave di esercizio KEYC (Global Encryption Key)
 * - authentication_key: chiave di autenticazione (Authentication Key)
 * - master_key: chiave master per key wrapping (opzionale)
 * - system_title: identificativo DLMS del dispositivo (8 byte)
 */
@Entity
@Table(name = "device_registry")
public class DeviceRegistry {

    /** Numero seriale del contatore (chiave primaria) */
    @Id
    @Column(length = 64)
    private String serialNumber;

    /** Nome logico del dispositivo DLMS (16 byte codificati) */
    @Column(length = 34)
    private String logicalDeviceName;

    /** Tipo dispositivo (es. RSE, RSV, HM_ICON, SSM_ICON) */
    @Column(length = 20)
    private String deviceType;

    /** Punto di riconsegna PDR (max 14 caratteri) */
    @Column(length = 14)
    private String meteringPointId;

    /** Chiave di cifratura DLMS (KEYC) - cifrata con AES-256-GCM */
    @Column(nullable = false)
    private byte[] encryptionKeyEnc;

    /** Chiave di autenticazione DLMS - cifrata con AES-256-GCM */
    @Column(nullable = false)
    private byte[] authenticationKeyEnc;

    /** Chiave master per key wrapping - cifrata con AES-256-GCM (opzionale) */
    private byte[] masterKeyEnc;

    /** System Title DLMS del dispositivo (8 byte) */
    @Column(nullable = false)
    private byte[] systemTitle;

    /** Frame counter per i messaggi inviati dal SAC al dispositivo */
    @Column(nullable = false)
    private long frameCounterTx = 0;

    /** Frame counter per i messaggi ricevuti dal dispositivo */
    @Column(nullable = false)
    private long frameCounterRx = 0;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public DeviceRegistry() {
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    // --- Getters e Setters ---

    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }

    public String getLogicalDeviceName() { return logicalDeviceName; }
    public void setLogicalDeviceName(String logicalDeviceName) { this.logicalDeviceName = logicalDeviceName; }

    public String getDeviceType() { return deviceType; }
    public void setDeviceType(String deviceType) { this.deviceType = deviceType; }

    public String getMeteringPointId() { return meteringPointId; }
    public void setMeteringPointId(String meteringPointId) { this.meteringPointId = meteringPointId; }

    public byte[] getEncryptionKeyEnc() { return encryptionKeyEnc; }
    public void setEncryptionKeyEnc(byte[] encryptionKeyEnc) { this.encryptionKeyEnc = encryptionKeyEnc; }

    public byte[] getAuthenticationKeyEnc() { return authenticationKeyEnc; }
    public void setAuthenticationKeyEnc(byte[] authenticationKeyEnc) { this.authenticationKeyEnc = authenticationKeyEnc; }

    public byte[] getMasterKeyEnc() { return masterKeyEnc; }
    public void setMasterKeyEnc(byte[] masterKeyEnc) { this.masterKeyEnc = masterKeyEnc; }

    public byte[] getSystemTitle() { return systemTitle; }
    public void setSystemTitle(byte[] systemTitle) { this.systemTitle = systemTitle; }

    public long getFrameCounterTx() { return frameCounterTx; }
    public void setFrameCounterTx(long frameCounterTx) { this.frameCounterTx = frameCounterTx; }

    public long getFrameCounterRx() { return frameCounterRx; }
    public void setFrameCounterRx(long frameCounterRx) { this.frameCounterRx = frameCounterRx; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return "DeviceRegistry{serialNumber='" + serialNumber + "', deviceType='" + deviceType + "'}";
    }
}
