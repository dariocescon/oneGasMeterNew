package com.aton.proj.oneGasMeter.entity;

/**
 * Stato di un comando da inviare al contatore.
 */
public enum CommandStatus {
    /** Comando in attesa di esecuzione */
    PENDING,
    /** Comando in fase di esecuzione */
    IN_PROGRESS,
    /** Comando eseguito con successo */
    DONE,
    /** Comando fallito */
    FAILED
}
