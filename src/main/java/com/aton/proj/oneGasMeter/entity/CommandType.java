package com.aton.proj.oneGasMeter.entity;

/**
 * Tipi di comandi supportati per i contatori gas.
 * Per aggiungere un nuovo comando:
 * 1. Aggiungere il valore a questo enum
 * 2. Implementare il caso nel metodo executeCommand di MeterSessionHandler
 */
public enum CommandType {
    /** Sincronizza l'orologio del contatore con il server */
    SYNC_CLOCK,
    /** Imposta l'orologio del contatore ad un orario specifico (payload: ISO timestamp) */
    SET_CLOCK,
    /** Chiudi la valvola del gas */
    DISCONNECT_VALVE,
    /** Riapri la valvola del gas */
    RECONNECT_VALVE,
    /** Cambia la destinazione push del contatore (payload: {"ip":"...", "port":...}) */
    CHANGE_PUSH_DESTINATION,
    /** Leggi il profilo di carico (payload: {"from":"ISO", "to":"ISO"}) */
    READ_LOAD_PROFILE,
    /** Leggi il log eventi del contatore */
    READ_EVENT_LOG
}
