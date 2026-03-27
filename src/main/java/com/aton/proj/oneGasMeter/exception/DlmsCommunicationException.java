package com.aton.proj.oneGasMeter.exception;

/**
 * Eccezione per errori di comunicazione DLMS/COSEM.
 */
public class DlmsCommunicationException extends RuntimeException {

    private final Integer errorCode;

    public DlmsCommunicationException(String message) {
        super(message);
        this.errorCode = null;
    }

    public DlmsCommunicationException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = null;
    }

    public DlmsCommunicationException(String message, int errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public DlmsCommunicationException(String message, Throwable cause, int errorCode) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    /** Codice errore DLMS, null se non disponibile */
    public Integer getErrorCode() {
        return errorCode;
    }
}
