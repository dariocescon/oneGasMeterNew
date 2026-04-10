package com.aton.proj.oneGasMeter.cosem;

/**
 * Codici evento GdM secondo UNI/TS 11291-12-2:2020, Appendice D.
 *
 * Ogni codice ha un valore numerico (1-181), una descrizione e un flag
 * che indica se deve comparire nel logbook metrologico.
 *
 * Codici > 192 sono riservati ai fabbricanti.
 */
public enum EventCode {

    // Dispositivo
    DEVICE_RESET(1, true, "Reset dispositivo"),
    METRO_LOG_RESET(2, true, "Reset registro eventi metrologici"),
    EVENT_LOG_RESET(3, false, "Reset registro eventi"),

    // Input ausiliari
    AUX_INPUT_ON(4, false, "Input ausiliario transizione off->on"),
    AUX_INPUT_OFF(5, false, "Input ausiliario transizione on->off"),
    AUX_WIRE_ON(6, false, "Input rilevazione continuita' cavi off->on"),
    AUX_WIRE_OFF(7, false, "Input rilevazione continuita' cavi on->off"),

    // Piano tariffario
    NEW_TARIFF_ACTIVATED(8, false, "Attivazione nuovo piano tariffario"),
    NEW_TARIFF_PROGRAMMED(9, false, "Programmazione nuovo piano tariffario"),

    // Orologio
    CLOCK_SYNC_FAILED(10, true, "Sincronizzazione orologio fallita"),
    CLOCK_SET_UNCONFIGURED(11, true, "Impostazione orologio nello stato non configurato"),
    CLOCK_SYNCED(12, true, "Sincronizzazione orologio"),

    // Sessione locale
    LOCAL_SESSION_START(13, false, "Sessione comunicazione locale: inizio"),
    LOCAL_SESSION_END(14, false, "Sessione comunicazione locale: fine"),

    // Configurazione
    METRO_PARAM_CONFIG(15, true, "Configurazione parametro metrologico"),
    NON_METRO_PARAM_CONFIG(16, false, "Configurazione parametro non metrologico"),
    CONFIG_SESSION_START(17, false, "Configurazione: inizio sessione programmazione"),
    CONFIG_SESSION_OK(18, false, "Configurazione: chiusura sessione con successo"),
    CONFIG_SESSION_FAIL(19, false, "Configurazione: chiusura sessione fallita"),

    // Errori misura
    MEASURE_ALGO_ERROR_START(20, true, "Errore algoritmo di misura: inizio"),
    MEASURE_ALGO_ERROR_END(21, true, "Errore algoritmo di misura: fine"),
    GENERAL_DEVICE_ERROR_START(22, true, "Errore generale dispositivo: inizio"),
    GENERAL_DEVICE_ERROR_END(23, true, "Errore generale dispositivo: fine"),
    NO_DIFF_PRESSURE_START(24, false, "Nessun valore pressione differenziale: inizio"),
    NO_DIFF_PRESSURE_END(25, false, "Nessun valore pressione differenziale: fine"),
    METRO_LOG_FULL(26, true, "Logbook metrologico pieno"),
    METRO_LOG_90PCT(27, true, "Logbook metrologico >= 90%"),
    EXT_MEASURE_ERROR_START(28, true, "Errore unita' misura esterna: inizio"),
    EXT_MEASURE_ERROR_END(29, true, "Errore unita' misura esterna: fine"),

    // Valvola - comandi
    VALVE_CLOSED_REMOTE(30, true, "Valvola chiusa per comando remoto/locale"),
    VALVE_OPENED(31, true, "Valvola portata allo stato aperto"),

    // Gas - fattore conversione
    CONV_FACTOR_ABOVE_MAX_START(32, true, "Fattore conversione sopra soglia max: inizio"),
    CONV_FACTOR_ABOVE_MAX_END(33, true, "Fattore conversione sopra soglia max: fine"),
    CONV_FACTOR_BELOW_MIN_START(34, true, "Fattore conversione sotto soglia min: inizio"),
    CONV_FACTOR_BELOW_MIN_END(35, true, "Fattore conversione sotto soglia min: fine"),

    // Gas - portata
    FLOW_BELOW_MIN_START(36, true, "Portata sotto soglia min: inizio"),
    FLOW_BELOW_MIN_END(37, true, "Portata sotto soglia min: fine"),
    FLOW_ABOVE_QMAX_START(38, true, "Portata sopra Qmax: inizio"),
    FLOW_ABOVE_QMAX_END(39, true, "Portata sopra Qmax: fine"),
    OVERFLOW_START(40, true, "Condizione di overflow: inizio"),
    OVERFLOW_END(41, true, "Condizione di overflow: fine"),
    REVERSE_FLOW_START(42, true, "Flusso inverso gas: inizio"),
    REVERSE_FLOW_END(43, true, "Flusso inverso gas: fine"),

    // Gas - pressione
    PRESSURE_ABOVE_APP_MAX_START(44, true, "Pressione sopra soglia max applicazione: inizio"),
    PRESSURE_ABOVE_APP_MAX_END(45, true, "Pressione sopra soglia max applicazione: fine"),
    PRESSURE_BELOW_APP_MIN_START(46, true, "Pressione sotto soglia min applicazione: inizio"),
    PRESSURE_BELOW_APP_MIN_END(47, true, "Pressione sotto soglia min applicazione: fine"),
    PRESSURE_ABOVE_PHYS_START(48, true, "Pressione sopra max fisico: inizio"),
    PRESSURE_ABOVE_PHYS_END(49, true, "Pressione sopra max fisico: fine"),
    PRESSURE_BELOW_PHYS_START(50, true, "Pressione sotto min fisico: inizio"),
    PRESSURE_BELOW_PHYS_END(51, true, "Pressione sotto min fisico: fine"),
    PRESSURE_FAULT_START(52, true, "Pressione guasta: inizio"),
    PRESSURE_FAULT_END(53, true, "Pressione guasta: fine"),

    // Gas - temperatura
    TEMP_ABOVE_APP_MAX_START(54, true, "Temperatura sopra soglia max applicazione: inizio"),
    TEMP_ABOVE_APP_MAX_END(55, true, "Temperatura sopra soglia max applicazione: fine"),
    TEMP_BELOW_APP_MIN_START(56, true, "Temperatura sotto soglia min applicazione: inizio"),
    TEMP_BELOW_APP_MIN_END(57, true, "Temperatura sotto soglia min applicazione: fine"),
    TEMP_ABOVE_PHYS_START(58, true, "Temperatura sopra max fisico: inizio"),
    TEMP_ABOVE_PHYS_END(59, true, "Temperatura sopra max fisico: fine"),
    TEMP_BELOW_PHYS_START(60, true, "Temperatura sotto min fisico: inizio"),
    TEMP_BELOW_PHYS_END(61, true, "Temperatura sotto min fisico: fine"),
    TEMP_FAULT_START(62, true, "Temperatura guasta: inizio"),
    TEMP_FAULT_END(63, true, "Temperatura guasta: fine"),

    // Altre anomalie
    GAS_CHROMO_FAULT_START(64, false, "Guasto gascromatografo: inizio"),
    GAS_CHROMO_FAULT_END(65, false, "Guasto gascromatografo: fine"),
    MEMORY_FAULT(66, true, "Memoria guasta"),
    DEVICE_STATE_CHANGED(67, true, "Modificato stato UNI/TS del dispositivo"),
    PASSWORD_CHANGED(68, true, "Password modificata"),
    PASSWORD_DEFAULT_RESTORED(69, true, "Password di default ripristinata"),

    // Alimentazione
    POWER_CRITICAL_START(70, true, "Alimentazione mancanza critica: inizio"),
    POWER_CRITICAL_END(71, true, "Alimentazione mancanza critica: fine"),
    POWER_PRIMARY_LOST_START(72, false, "Alimentazione primaria persa: inizio"),
    POWER_PRIMARY_LOST_END(73, false, "Alimentazione primaria persa: fine"),
    BATTERY_BELOW_10PCT_START(74, false, "Batteria < 10%: inizio"),
    BATTERY_BELOW_10PCT_END(75, false, "Batteria < 10%: fine (batteria sostituita)"),
    AC_POWER_LOST_30MIN_START(76, false, "Mancanza rete CA > 30 min: inizio"),
    AC_POWER_LOST_30MIN_END(77, false, "Mancanza rete CA > 30 min: fine"),

    // Guasti dispositivo
    PRIMARY_METER_FAULT_START(78, true, "Guasto misuratore primario: inizio"),
    PRIMARY_METER_FAULT_END(79, true, "Guasto misuratore primario: fine"),

    // Frode
    TAMPER_DETECTED_START(80, true, "Rilevata manomissione: inizio"),
    TAMPER_DETECTED_END(81, true, "Rilevata manomissione: fine"),

    // Stampa
    PRINT_ERROR_START(82, false, "Errore stampa: inizio"),
    PRINT_ERROR_END(83, false, "Errore stampa: fine"),

    // Comunicazione
    REMOTE_COMM_MODULE_FAULT(84, false, "Guasto modulo comunicazione remota"),
    SOFTWARE_CRITICAL_ERROR(85, false, "Errore grave del software"),

    // Ora legale
    DST_START(86, false, "Ora legale: inizio"),
    DST_END(87, false, "Ora legale: fine"),

    // Push
    PUSH_ERROR_START(88, false, "Errore operazione push: inizio"),
    PUSH_ERROR_END(89, false, "Errore operazione push: fine"),

    // Fatturazione
    EOB_PERIODIC(90, true, "Chiusura periodo fatturazione periodica"),
    EOB_TARIFF_CHANGE(91, true, "Chiusura periodo fatturazione per modifica piano tariffario"),
    EOB_LOCAL_REQUEST(92, true, "Chiusura periodo fatturazione su richiesta locale"),
    EOB_REMOTE_REQUEST(93, true, "Chiusura periodo fatturazione su richiesta remota"),

    // Batteria critica
    BATTERY_CRITICAL_START(94, true, "Batteria sotto livello critico: inizio"),
    BATTERY_CRITICAL_END(95, true, "Batteria sotto livello critico: fine"),

    // Firmware
    FW_UPDATE_STARTED(96, true, "Nuovo aggiornamento firmware iniziato"),
    FW_UPDATE_INIT(97, true, "Inizializzazione processo aggiornamento firmware"),
    FW_VERIFY_OK(98, true, "Aggiornamento firmware: verifica OK"),
    FW_VERIFY_FAILED(99, true, "Aggiornamento firmware: verifica fallita"),
    FW_ACTIVATE_OK(100, true, "Aggiornamento firmware: attivazione con successo"),
    FW_ACTIVATE_FAILED(101, true, "Aggiornamento firmware: attivazione fallita"),

    // Valvola - cause chiusura
    VALVE_CLOSED_LEAK(102, true, "Valvola chiusa a causa di perdite"),
    VALVE_CLOSED_BATTERY_REMOVED(103, true, "Valvola chiusa per batteria rimossa"),
    VALVE_CLOSED_BATTERY_CRITICAL(104, true, "Valvola chiusa per carica residua sotto livello critico"),
    VALVE_CLOSED_MEASURE_FAULT(105, true, "Valvola chiusa per guasto sistema misurazione"),
    VALVE_INVALID_PASSWORD(106, false, "Password valvola non valida"),
    VALVE_CLOSED_NO_COMMS(107, true, "Valvola chiusa per nessuna comunicazione"),
    VALVE_NEW_PASSWORD(108, false, "Nuova password valvola programmata"),
    VALVE_READY_VALID_PWD(109, false, "Valvola pronta per connessione (password valida)"),
    VALVE_READY_COMMAND(110, false, "Valvola pronta per connessione (comando)"),
    VALVE_OPEN_PERIOD_START(111, false, "Valvola: inizio periodo validita' apertura"),
    VALVE_OPEN_PERIOD_END(112, false, "Valvola: fine periodo validita' apertura"),
    VALVE_CLOSED_LEAKING(113, true, "Valvola chiusa con perdite presenti"),
    VALVE_CANNOT_OPERATE(114, true, "Valvola: impossibile aprire o chiudere"),
    MODULE_DISCONNECTION(115, false, "Disconnessione moduli fisici"),

    // Sicurezza
    EXT_INTERFERENCE_START(116, true, "Campo interferente esterno: inizio"),
    EXT_INTERFERENCE_END(117, true, "Campo interferente: scomparsa"),
    ELECTRONICS_ACCESS(118, true, "Accesso a parte elettronica"),
    DECRYPTION_ERROR(119, true, "Decrittazione: errore"),
    AUTHENTICATION_ERROR(120, true, "Autenticazione: errore"),
    UNAUTHORIZED_ACCESS(121, true, "Accesso non autorizzato"),
    CRYPTO_KEY_PROGRAMMED(122, true, "Programmazione chiavi crittografiche"),
    CRYPTO_KEY_ACTIVATED(123, true, "Attivazione chiavi crittografiche"),
    BATTERY_UNAUTHORIZED_REMOVAL(124, true, "Rimozione non autorizzata della batteria"),

    // Database
    DATABASE_RESET(125, true, "Database reset"),
    DATABASE_RESET_AFTER_FW(126, true, "Database reset dopo aggiornamento firmware"),
    DATABASE_CORRUPTED(127, true, "Database corrotto"),

    // Aggiornamento chiavi
    MASTER_KEY_UPDATED(128, true, "Aggiornata Master Key"),
    MGMT_KEY_UPDATED(129, true, "Aggiornata chiave Management (KEYC)"),
    IM_KEY_UPDATED(130, true, "Aggiornata chiave Installer/Maintenance (KEYT)"),
    GA_KEY_UPDATED(131, true, "Aggiornata chiave Guarantor Authority (KEYS)"),
    GATEWAY_KEY_UPDATED(132, false, "Aggiornata chiave Gateway (KEYN)"),
    BROADCAST_KEY_UPDATED(133, false, "Aggiornata chiave Broadcasting (KEYM)"),

    // PM1 / Comunicazione
    PM1_POWER_INCREASED(134, false, "PM1 Power Level: incrementato"),
    PM1_POWER_DECREASED(135, false, "PM1 Power Level: diminuito"),
    PM1_POWER_MAX(136, false, "PM1 Power Level: raggiunto il massimo"),
    PM1_POWER_MIN(137, false, "PM1 Power Level: raggiunto il minimo"),
    PM1_CHANNEL_CHANGED(138, false, "PM1 Channel: modificato"),
    ACTIVE_MODE_START(139, false, "PM1/PP4 Active mode: inizio"),
    ACTIVE_MODE_END(140, false, "PM1/PP4 Active mode: fine"),
    ORPHAN_MODE_START(141, false, "PM1/PP4 Orphan mode: inizio"),
    ORPHAN_MODE_END(142, false, "PM1/PP4 Orphan mode: fine"),
    // 143 riservato
    PM1_PIB_UPDATED(144, false, "PM1: PIB aggiornato"),
    PM1_MIB_UPDATED(145, false, "PM1: MIB aggiornato"),
    PM1_SYNC_ACCESS_CHANGED(146, false, "PM1: accesso sincrono modificato"),
    PM1_SYNC_PERIOD_CHANGED(147, false, "PM1: periodo sincrono modificato"),
    PM1_MAINT_WINDOW_CHANGED(148, false, "PM1: maintenance window modificata"),
    ORPHAN_THRESHOLD_CHANGED(149, false, "PM1/PP4 Orphan mode: soglia modificata"),
    PM1_AFFILIATION_CHANGED(150, false, "PM1: parametri affiliazione modificati"),
    RF_SECONDARY_ADDR_CHANGED(151, false, "Indirizzo secondario modulo RF modificato"),

    // Configurazione dispositivo
    VALVE_PGV_MODIFIED(152, false, "Valvola: configurazione PGV modificata"),
    GAS_DAY_START_UPDATED(153, false, "Ora inizio giorno gas aggiornata"),
    BILLING_PERIOD_UPDATED(154, false, "Periodo di fatturazione aggiornato"),
    PUSH_SCHEDULER_1_MODIFIED(155, false, "Push Scheduler 1: modificato"),
    PUSH_SETUP_1_MODIFIED(156, false, "Push Setup 1: modificato"),
    PUSH_SCHEDULER_2_MODIFIED(157, false, "Push Scheduler 2: modificato"),
    PUSH_SETUP_2_MODIFIED(158, false, "Push Setup 2: modificato"),
    PUSH_SCHEDULER_3_MODIFIED(159, false, "Push Scheduler 3: modificato"),
    PUSH_SETUP_3_MODIFIED(160, false, "Push Setup 3: modificato"),
    PUSH_SCHEDULER_4_MODIFIED(161, false, "Push Scheduler 4: modificato"),
    PUSH_SETUP_4_MODIFIED(162, false, "Push Setup 4: modificato"),
    IM_USER_MODIFIED(163, false, "Installatore/Manutentore: utente modificato"),
    IM_ACTIVATED(164, false, "Installatore/Manutentore: attivazione"),
    FC_THRESHOLDS_MODIFIED(165, false, "Soglie Frame Counter: modificate"),
    CLOCK_PARAMS_MODIFIED(166, false, "Parametri orologio modificati"),
    SYNC_ALGO_MODIFIED(167, false, "Algoritmo di sincronizzazione modificato"),
    PDR_MODIFIED(168, false, "Identificatore punto di misura (PDR) modificato"),
    BASE_TEMP_MODIFIED(169, false, "Temperatura base modificata"),
    FALLBACK_TEMP_MODIFIED(170, false, "Temperatura di rimpiazzo modificata"),

    // Connessione remota
    REMOTE_CONN_START(171, false, "Connessione remota: inizio"),
    REMOTE_CONN_END(172, false, "Connessione remota: fine"),
    REMOTE_CONN_TIMEOUT(173, false, "Connessione remota: time-out"),

    // Manutenzione
    MAINT_WINDOW_HW_FAIL(174, false, "Finestra manutenzione: fallimento hardware"),
    MAINT_WINDOW_SW_FAIL(175, false, "Finestra manutenzione: fallimento software"),
    MAINT_WINDOW_ACTIVATED(176, false, "Finestra manutenzione: attivazione"),
    MAINT_WINDOW_TERMINATED(177, false, "Finestra manutenzione: terminazione"),
    MANUAL_REMOTE_ACTIVATION(178, false, "Attivazione manuale connessione remota"),

    // Valvola - chiusure automatiche
    VALVE_CLOSED_MAX_TAMPER(179, true, "Valvola chiusa per max tentativi frode"),
    VALVE_CLOSED_BATTERY_TIMEOUT(180, true, "Valvola chiusa per tempo eccessivo sostituzione batteria"),
    IM_ASSOCIATION_DISABLED(181, false, "Associazione Installer/Maintainer disabilitata");

    private final int code;
    private final boolean metrological;
    private final String description;

    EventCode(int code, boolean metrological, String description) {
        this.code = code;
        this.metrological = metrological;
        this.description = description;
    }

    public int getCode() { return code; }
    public boolean isMetrological() { return metrological; }
    public String getDescription() { return description; }

    /**
     * Trova un EventCode per codice numerico.
     *
     * @param code codice evento (1-181)
     * @return l'EventCode corrispondente, o null se non trovato (>192 = fabbricante)
     */
    public static EventCode findByCode(int code) {
        for (EventCode ec : values()) {
            if (ec.code == code) return ec;
        }
        return null;
    }

    /**
     * Restituisce la descrizione di un codice evento, inclusi quelli dei fabbricanti.
     *
     * @param code codice evento
     * @return descrizione leggibile
     */
    public static String describeCode(int code) {
        EventCode ec = findByCode(code);
        if (ec != null) return ec.description;
        if (code > 192) return "Evento fabbricante (" + code + ")";
        return "Codice evento sconosciuto (" + code + ")";
    }
}
