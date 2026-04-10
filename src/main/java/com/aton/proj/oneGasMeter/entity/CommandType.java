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

    // --- Configurazione dispositivo ---
    /** Esegue uno script del Global Script (payload: script ID come numero) */
    EXECUTE_SCRIPT
}
