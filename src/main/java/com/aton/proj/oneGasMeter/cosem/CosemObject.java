package com.aton.proj.oneGasMeter.cosem;

import java.util.Optional;

/**
 * Catalogo degli oggetti COSEM per contatori gas italiani secondo UNI/TS 11291-12-2:2020.
 *
 * Ogni entry associa un codice OBIS al suo Class ID, indice attributo e descrizione.
 * Per aggiungere un nuovo oggetto, aggiungere una nuova entry a questo enum.
 *
 * Class ID di riferimento:
 *   1    = Data
 *   3    = Register
 *   4    = ExtendedRegister
 *   7    = ProfileGeneric
 *   8    = Clock
 *   9    = ScriptTable
 *   15   = AssociationLN
 *   18   = ImageTransfer
 *   21   = LimiterThreshold
 *   22   = SingleActionSchedule
 *   40   = PushSetup
 *   62   = CompactData
 *   64   = SecuritySetup
 *   70   = DisconnectControl
 *   8192 = UNI/TS 11291 TariffPlan (proprietaria)
 */
public enum CosemObject {

    // ========== IDENTIFICAZIONE ==========
    SERIAL_NUMBER("0.0.96.1.0.255", 1, 2, "Numero seriale del contatore"),
    LOGICAL_DEVICE_NAME("0.0.42.0.0.255", 1, 2, "Nome logico del dispositivo"),
    DEVICE_TYPE_ID("0.0.96.1.3.255", 1, 2, "Identificativo tipo dispositivo"),
    DEVICE_TYPE_ID_2("0.0.96.1.4.255", 1, 2, "Identificativo tipo dispositivo 2"),
    METERING_POINT_ID("0.0.96.1.10.255", 1, 2, "Punto di riconsegna PDR"),
    UTILITY_NUMBER("0.0.96.1.1.255", 1, 2, "Numero del distributore (opzionale)"),

    // ========== OROLOGIO E TEMPO ==========
    CLOCK("0.0.1.0.0.255", 8, 2, "Orologio del contatore"),
    UNIX_TIME("0.0.1.1.0.255", 1, 2, "Tempo UNIX (secondi da epoch)"),
    START_GAS_DAY("7.0.0.9.3.255", 1, 2, "Ora inizio giorno gas convenzionale"),
    SYNC_ALGORITHM("0.0.94.39.44.255", 1, 2, "Algoritmo di sincronizzazione orologio"),

    // ========== TOTALIZZATORI VOLUME ==========
    CURRENT_INDEX_CONVERTED_VOL("7.0.13.2.0.255", 3, 2, "Totalizzatore corrente volumi convertiti (Sm3)"),
    CURRENT_INDEX_CONV_VOL_ALARM("7.0.12.2.0.255", 3, 2, "Totalizzatore corrente volumi convertiti in allarme"),
    HIGH_RES_INDEX_CONV_VOL("7.128.13.2.0.255", 3, 2, "Totalizzatore alta risoluzione volumi convertiti"),
    CURRENT_INDEX_CONV_VOL_F1("7.0.13.2.1.255", 3, 2, "Totalizzatore volumi convertiti - Fascia F1"),
    CURRENT_INDEX_CONV_VOL_F2("7.0.13.2.2.255", 3, 2, "Totalizzatore volumi convertiti - Fascia F2"),
    CURRENT_INDEX_CONV_VOL_F3("7.0.13.2.3.255", 3, 2, "Totalizzatore volumi convertiti - Fascia F3"),

    // ========== PORTATA ==========
    CONV_GAS_FLOW("7.0.43.45.0.255", 4, 2, "Portata convertita convenzionale (Sm3/h)"),

    // ========== DIAGNOSTICA E BATTERIA ==========
    DAILY_DIAGNOSTIC("7.1.96.5.1.255", 1, 2, "Diagnostica giornaliera (bitmask)"),
    METROLOGICAL_EVENT_COUNTER("0.0.96.15.1.255", 1, 2, "Contatore eventi metrologici"),
    EVENT_COUNTER("0.0.96.15.2.255", 1, 2, "Contatore eventi generali"),
    ERROR_REGISTER("0.0.97.97.0.255", 1, 2, "Registro errori"),
    COMM_TAMPER_EVENT_COUNTER("0.0.96.20.30.255", 1, 2, "Contatore eventi manomissione comunicazione"),
    MONITORING_COMM_DATA("0.0.94.39.56.255", 1, 2, "Dati monitoraggio comunicazione"),
    MONITORING_SLA_DATA("0.0.94.39.59.255", 1, 2, "Dati monitoraggio SLA"),
    PP4_NETWORK_STATUS("0.1.96.5.4.255", 1, 2, "Stato rete PP4"),
    BATTERY_USE_TIME_0("0.0.96.6.0.255", 3, 2, "Tempo utilizzo batteria 0 (minuti)"),
    BATTERY_USE_TIME_1("0.1.96.6.0.255", 3, 2, "Tempo utilizzo batteria 1 (minuti)"),
    BATTERY_REMAINING_0("0.0.96.6.6.255", 3, 2, "Capacita' residua batteria 0 (minuti)"),
    BATTERY_REMAINING_1("0.1.96.6.6.255", 3, 2, "Capacita' residua batteria 1 (minuti)"),
    TOTAL_OPERATING_TIME("0.0.96.8.0.255", 3, 2, "Tempo operativo totale (minuti)"),
    BATTERY_INITIAL_CAPACITY_0("0.0.96.6.4.255", 3, 2, "Capacita' iniziale batteria 0"),
    BATTERY_INITIAL_CAPACITY_1("0.1.96.6.4.255", 3, 2, "Capacita' iniziale batteria 1"),

    // ========== VALVOLA ==========
    VALVE_STATE("0.0.96.3.10.255", 70, 2, "Stato valvola gas (DisconnectControl)"),
    VALVE_CONFIG_PGV("0.0.94.39.3.255", 1, 2, "Configurazione valvola PGV"),
    VALVE_MAX_PASSWORD_ATTEMPTS("0.0.94.39.2.255", 1, 2, "Tentativi massimi password valvola"),
    VALVE_ENABLE_PASSWORD("0.0.94.39.1.255", 1, 2, "Password abilitazione valvola"),
    VALVE_CLOSURE_CAUSE("0.0.94.39.7.255", 1, 2, "Causa chiusura valvola"),
    VALVE_OPENING_DURATION("0.0.94.39.6.255", 1, 2, "Durata validita' comando apertura"),
    DAYS_WITHOUT_COMMS_THRESHOLD("0.0.94.39.5.255", 21, 2, "Soglia giorni senza comunicazione"),
    TAMPERING_ATTEMPTS_THRESHOLD("0.0.94.39.25.255", 21, 2, "Soglia tentativi manomissione"),
    LEAKAGE_TEST_PARAMS("0.0.94.39.26.255", 1, 2, "Parametri test perdite"),
    VALVE_SINGLE_ACTION_SCHEDULE("0.0.15.0.1.255", 22, 2, "Schedule azione valvola"),

    // ========== FATTURAZIONE (EOB) ==========
    BILLING_PERIOD_COUNTER("7.0.0.1.0.255", 1, 2, "Contatore periodo fatturazione"),
    EOB_SNAPSHOT_PERIOD("7.0.0.8.23.255", 1, 2, "Periodo snapshot EOB (giorni)"),
    EOB_SNAPSHOT_STARTING_DATE("0.0.94.39.11.255", 1, 2, "Data inizio snapshot EOB"),
    ON_DEMAND_SNAPSHOT_TIME("0.0.94.39.8.255", 1, 2, "Istante snapshot su richiesta"),

    // ========== PIANO TARIFFARIO ==========
    ACTIVE_TARIFF_PLAN("0.0.94.39.21.255", 8192, 2, "Piano tariffario attivo"),
    PASSIVE_TARIFF_PLAN("0.0.94.39.22.255", 8192, 2, "Piano tariffario passivo"),

    // ========== PROFILI E LOG ==========
    DAILY_LOAD_PROFILE("7.0.99.99.3.255", 7, 2, "Profilo di carico giornaliero"),
    METROLOGICAL_LOGBOOK("7.0.99.98.1.255", 7, 2, "Registro eventi metrologici"),
    SNAPSHOT_PERIOD_DATA("7.0.98.11.0.255", 7, 2, "Dati snapshot periodo fatturazione"),
    PARAMETER_MONITOR_LOGBOOK("7.0.99.16.0.255", 7, 2, "Registro monitor parametri"),

    // ========== PUSH SETUP (4 istanze, priorita' crescente) ==========
    PUSH_SETUP_1("0.1.25.9.0.255", 40, 2, "Push Setup 1 - fatturazione"),
    PUSH_SETUP_2("0.2.25.9.0.255", 40, 2, "Push Setup 2 - dati giornalieri"),
    PUSH_SETUP_3("0.3.25.9.0.255", 40, 2, "Push Setup 3 - SLA riapertura valvola"),
    PUSH_SETUP_4("0.4.25.9.0.255", 40, 2, "Push Setup 4 - orphan mode"),
    PUSH_SCHEDULER_1("0.1.15.0.4.255", 22, 4, "Push Scheduler 1"),
    PUSH_SCHEDULER_2("0.2.15.0.4.255", 22, 4, "Push Scheduler 2"),
    PUSH_SCHEDULER_3("0.3.15.0.4.255", 22, 4, "Push Scheduler 3"),
    PUSH_SCHEDULER_4("0.4.15.0.4.255", 22, 4, "Push Scheduler 4"),

    // ========== FRAME COUNTER ==========
    MGMT_FRAME_COUNTER_ONLINE("0.0.43.1.1.255", 1, 2, "Frame counter gestione on-line"),
    MGMT_FRAME_COUNTER_OFFLINE("0.1.43.1.1.255", 1, 2, "Frame counter gestione off-line"),
    GA_FRAME_COUNTER("0.0.43.1.48.255", 1, 2, "Frame counter Autorita' Garante"),
    IM_FRAME_COUNTER("0.0.43.1.3.255", 1, 2, "Frame counter Installatore/Manutentore"),
    GLOBAL_FC_THRESHOLDS("0.0.94.39.33.255", 1, 2, "Soglie globali frame counter"),

    // ========== ASSOCIAZIONI ==========
    MGMT_ASSOCIATION("0.0.40.0.1.255", 15, 2, "Associazione Management"),
    PUBLIC_ASSOCIATION("0.0.40.0.2.255", 15, 2, "Associazione Public"),
    IM_ASSOCIATION("0.0.40.0.3.255", 15, 2, "Associazione Installatore/Manutentore"),
    GA_ASSOCIATION("0.0.40.0.48.255", 15, 2, "Associazione Autorita' Garante"),
    BROADCAST_ASSOCIATION("0.0.40.0.32.255", 15, 2, "Associazione Broadcast"),

    // ========== SECURITY SETUP ==========
    MGMT_SECURITY_SETUP("0.0.43.0.1.255", 64, 2, "Security Setup Management"),
    IM_SECURITY_SETUP("0.0.43.0.3.255", 64, 2, "Security Setup Installatore/Manutentore"),
    GA_SECURITY_SETUP("0.0.43.0.48.255", 64, 2, "Security Setup Autorita' Garante"),
    BROADCAST_SECURITY_SETUP("0.0.43.0.32.255", 64, 2, "Security Setup Broadcast"),

    // ========== CONFIGURAZIONE DISPOSITIVO ==========
    GLOBAL_SCRIPT("0.0.10.0.0.255", 9, 2, "Script globale (configurazione dispositivo)"),
    IM_SETUP("0.0.94.39.30.255", 1, 2, "Setup Installatore/Manutentore"),
    IM_REMAINING_TIME("0.0.94.39.31.255", 1, 2, "Tempo residuo sessione I/M"),
    BATTERY_CHANGE_AUTH("0.0.94.39.14.255", 3, 2, "Autorizzazione cambio batteria"),
    ORPHANED_THRESHOLD("0.0.94.39.10.255", 1, 2, "Soglia orphan mode"),

    // ========== FIRMWARE ==========
    IMAGE_TRANSFER("0.0.44.0.0.255", 18, 6, "Image Transfer (aggiornamento firmware)"),

    // ========== COMUNICAZIONE PP4 ==========
    TIMEOUT_GPRS("0.0.94.39.52.255", 1, 2, "Timeout comunicazione GPRS"),
    TIMEOUT_NBIOT("0.1.94.39.52.255", 1, 2, "Timeout comunicazione NB-IoT"),

    // ========== COMPACT FRAME (Class 62) ==========
    CF1_ASSET_DATA("0.0.66.0.1.255", 62, 2, "CF1 - Dati anagrafici"),
    CF2_CLOCK_BEHAVIOUR("0.0.66.0.2.255", 62, 2, "CF2 - Parametri orologio"),
    CF3_DIAGNOSTICS("0.0.66.0.3.255", 62, 2, "CF3 - Diagnostica e allarmi"),
    CF4_EOB_PARAMS("0.0.66.0.4.255", 62, 2, "CF4 - Parametri fine fatturazione"),
    CF5_ACTIVE_TARIFF("0.0.66.0.5.255", 62, 2, "CF5 - Piano tariffario attivo"),
    CF6_PASSIVE_TARIFF("0.0.66.0.6.255", 62, 2, "CF6 - Piano tariffario passivo"),
    CF7_VALVE_PROGRAMMING("0.0.66.0.7.255", 62, 2, "CF7 - Programmazione valvola"),
    CF8_VALVE_STATUS("0.0.66.0.8.255", 62, 2, "CF8 - Stato valvola"),
    CF9_VALVE_MANAGEMENT("0.0.66.0.9.255", 62, 2, "CF9 - Gestione valvola"),
    CF14_DAILY_PROFILE("0.0.66.0.14.255", 62, 2, "CF14 - Profilo giornaliero"),
    CF15_EVENT_LOG("0.0.66.0.15.255", 62, 2, "CF15 - Log eventi metrologici"),
    CF16_BILLING_DATA("0.0.66.0.16.255", 62, 2, "CF16 - Dati fatturazione"),
    CF22_FW_STATUS("0.0.66.0.22.255", 62, 2, "CF22 - Stato trasferimento firmware"),
    CF41_COMM_SETUP("0.0.66.0.41.255", 62, 2, "CF41 - Configurazione comunicazione PP4"),
    CF47_CONTENT_A("0.0.66.0.47.255", 62, 2, "CF47 - Push Content A"),
    CF48_CONTENT_B("0.0.66.0.48.255", 62, 2, "CF48 - Push Content B"),
    CF49_CONTENT_C("0.0.66.0.49.255", 62, 2, "CF49 - Push Content C"),
    CF51_FRAME_COUNTERS("0.0.66.0.51.255", 62, 2, "CF51 - Valori frame counter");

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

    public String getObisCode() { return obisCode; }
    public int getClassId() { return classId; }
    public int getAttributeIndex() { return attributeIndex; }
    public String getDescription() { return description; }

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
     * Si leggono automaticamente: Data (1), Register (3), ExtendedRegister (4),
     * Clock (8), DisconnectControl (70).
     * NON si leggono automaticamente: profili (7), associazioni (15), security (64),
     * push (40), compact frame (62), script (9), schedule (22), tariff plan (8192),
     * image transfer (18), limiter (21).
     */
    public boolean isAutoReadable() {
        return classId == 1 || classId == 3 || classId == 4 || classId == 8 || classId == 70;
    }
}
