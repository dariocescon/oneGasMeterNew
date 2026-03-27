package com.aton.proj.oneGasMeter.cosem;

import java.util.Optional;

/**
 * Catalogo degli oggetti COSEM utilizzati dai contatori gas italiani.
 * Ogni entry associa un codice OBIS al suo Class ID, indice attributo e descrizione.
 *
 * Per aggiungere un nuovo oggetto COSEM, aggiungere una nuova entry a questo enum.
 *
 * Class ID di riferimento:
 *   1  = Data
 *   3  = Register
 *   7  = ProfileGeneric
 *   8  = Clock
 *   15 = AssociationLN
 *   40 = PushSetup
 *   64 = SecuritySetup
 *   70 = DisconnectControl
 */
public enum CosemObject {

    // --- Identificazione ---
    SERIAL_NUMBER("0.0.96.1.0.255", 1, 2, "Numero seriale del contatore"),
    LOGICAL_DEVICE_NAME("0.0.42.0.0.255", 1, 2, "Nome logico del dispositivo"),
    FIRMWARE_VERSION("1.0.0.2.0.255", 1, 2, "Versione firmware"),
    PRODUCTION_DATE("0.0.96.1.4.255", 1, 2, "Data di produzione"),
    MANUFACTURER_ID("0.0.96.1.1.255", 1, 2, "Identificativo produttore"),

    // --- Orologio ---
    CLOCK("0.0.1.0.0.255", 8, 2, "Orologio del contatore"),

    // --- Misure Gas ---
    GAS_VOLUME_TOTAL("7.0.13.2.0.255", 3, 2, "Volume totale gas (m3)"),
    GAS_VOLUME_CORRECTED("7.0.13.2.1.255", 3, 2, "Volume corretto gas (Sm3 - PTZ)"),
    GAS_FLOW_RATE("7.0.43.0.0.255", 3, 2, "Portata istantanea (m3/h)"),
    GAS_PRESSURE("7.0.42.0.0.255", 3, 2, "Pressione gas (mbar)"),
    GAS_TEMPERATURE("7.0.41.0.0.255", 3, 2, "Temperatura gas (C)"),
    GAS_CONVERSION_FACTOR("7.0.52.0.0.255", 3, 2, "Fattore conversione PTZ"),

    // --- Stato dispositivo ---
    VALVE_STATE("0.0.96.3.10.255", 70, 2, "Stato valvola gas"),
    BATTERY_VOLTAGE("0.0.96.6.0.255", 3, 2, "Tensione batteria (V)"),
    ERROR_REGISTER("0.0.97.97.0.255", 1, 2, "Registro errori"),

    // --- Profili ---
    LOAD_PROFILE_1("7.0.99.99.1.255", 7, 2, "Profilo di carico giornaliero"),
    LOAD_PROFILE_2("7.0.99.99.2.255", 7, 2, "Profilo di carico mensile"),
    EVENT_LOG("0.0.99.98.0.255", 7, 2, "Log eventi"),

    // --- Comunicazione ---
    PUSH_SETUP("0.0.25.9.0.255", 40, 2, "Configurazione push"),

    // --- Associazione / Sicurezza ---
    ASSOCIATION_LN("0.0.40.0.0.255", 15, 2, "Associazione LN"),
    SECURITY_SETUP("0.0.43.0.0.255", 64, 2, "Configurazione sicurezza");

    private final String obisCode;
    private final int classId;
    private final int attributeIndex;
    private final String description;

    CosemObject(String obisCode, int classId, int attributeIndex, String description) {
        this.obisCode = obisCode;
        this.classId = classId;
        this.attributeIndex = attributeIndex;
        this.description = description;
    }

    public String getObisCode() {
        return obisCode;
    }

    public int getClassId() {
        return classId;
    }

    public int getAttributeIndex() {
        return attributeIndex;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Cerca un oggetto COSEM per codice OBIS.
     *
     * @param obisCode codice OBIS da cercare (es. "0.0.96.1.0.255")
     * @return Optional contenente l'oggetto se trovato
     */
    public static Optional<CosemObject> findByObisCode(String obisCode) {
        for (CosemObject obj : values()) {
            if (obj.obisCode.equals(obisCode)) {
                return Optional.of(obj);
            }
        }
        return Optional.empty();
    }

    /**
     * Verifica se questo oggetto e' leggibile automaticamente durante una sessione.
     * Esclude profili (Class 7), associazione (Class 15) e security setup (Class 64).
     */
    public boolean isAutoReadable() {
        return classId == 1 || classId == 3 || classId == 8 || classId == 70;
    }
}
