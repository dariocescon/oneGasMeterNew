package com.aton.proj.oneGasMeter.entity;

/**
 * Tipi di comandi supportati per i contatori gas.
 * Basato sui casi d'uso definiti in UNI/TS 11291-12-2:2020, Appendice C.
 *
 * Per aggiungere un nuovo comando:
 * 1. Aggiungere il valore a questo enum
 * 2. Implementare il caso nel metodo executeCommand di MeterSessionHandler
 */
public enum CommandType {

    // --- Orologio ---
    /** Sincronizza l'orologio del contatore con il server (UNIX Time) */
    SYNC_CLOCK,
    /** Imposta l'orologio del contatore ad un orario specifico (payload: ISO timestamp) */
    SET_CLOCK,

    // --- Valvola ---
    /** Chiudi la valvola del gas */
    DISCONNECT_VALVE,
    /** Riapri la valvola del gas */
    RECONNECT_VALVE,
    /** Imposta la password della valvola (payload: numero 0-65535) */
    SET_VALVE_PASSWORD,
    /** Imposta la durata di validita' del comando di apertura (payload: minuti) */
    SET_VALVE_OPENING_DURATION,

    // --- Push ---
    /** Cambia la destinazione push del contatore (payload: {"ip":"...", "port":...}) */
    CHANGE_PUSH_DESTINATION,

    // --- Lettura profili ---
    /** Leggi il profilo di carico (payload: {"profile":"daily|monthly", "from":"ISO", "to":"ISO"}) */
    READ_LOAD_PROFILE,
    /** Leggi il log eventi metrologici (payload: {"from":"ISO", "to":"ISO"}) */
    READ_EVENT_LOG,
    /** Leggi i dati di fatturazione (payload: {"from":"ISO", "to":"ISO"}) */
    READ_BILLING_DATA,

    // --- Diagnostica ---
    /** Leggi la compact frame diagnostica CF3 */
    READ_DIAGNOSTICS,
    /** Leggi lo stato della valvola CF8 */
    READ_VALVE_STATUS,

    // --- Fatturazione ---
    /** Forza la chiusura del periodo di fatturazione (script 8) */
    FORCE_EOB,

    // --- Configurazione remota ---
    /** Leggi parametri EOB (CF4) */
    READ_EOB_PARAMS,
    /** Scrivi parametri EOB (payload: byte[] base64 della CF4) */
    WRITE_EOB_PARAMS,
    /** Leggi piano tariffario attivo (CF5) */
    READ_ACTIVE_TARIFF,
    /** Scrivi piano tariffario passivo (payload: byte[] base64 della CF6) */
    WRITE_PASSIVE_TARIFF,
    /** Leggi configurazione comunicazione PP4 (CF41) */
    READ_COMM_SETUP,
    /** Scrivi configurazione comunicazione PP4 (payload: byte[] base64 della CF41) */
    WRITE_COMM_SETUP,

    // --- Firmware ---
    /** Inizia trasferimento firmware (payload: {"identifier":"nome", "size":12345, "image":"base64..."}) */
    FW_TRANSFER_INITIATE,
    /** Verifica immagine firmware trasferita */
    FW_VERIFY,
    /** Attiva immagine firmware verificata (il contatore si riavvia) */
    FW_ACTIVATE,
    /** Leggi stato trasferimento firmware (CF22) */
    READ_FW_STATUS,

    // --- Chiavi crittografiche ---
    /** Cambia chiave HLS di un'associazione (payload: {"association":"OBIS", "secret":"base64"}) */
    CHANGE_HLS_SECRET,
    /** Trasferimento chiave globale (payload: {"securitySetup":"OBIS", "wrappedKey":"base64"}) */
    GLOBAL_KEY_TRANSFER,

    // --- Installer/Maintainer ---
    /** Imposta permessi I/M (payload: bitmask come numero intero, vedi Appendice H) */
    SET_IM_PERMISSIONS,
    /** Leggi tempo residuo sessione I/M */
    READ_IM_REMAINING_TIME,

    // --- Configurazione dispositivo ---
    /** Esegue uno script del Global Script (payload: script ID come numero) */
    EXECUTE_SCRIPT
}
