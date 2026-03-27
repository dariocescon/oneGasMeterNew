package com.aton.proj.oneGasMeter.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test unitari per DlmsCommunicationException.
 */
class DlmsCommunicationExceptionTest {

    @Test
    void constructorWithMessage() {
        DlmsCommunicationException ex = new DlmsCommunicationException("errore test");
        assertEquals("errore test", ex.getMessage());
        assertNull(ex.getErrorCode());
        assertNull(ex.getCause());
    }

    @Test
    void constructorWithMessageAndCause() {
        Exception cause = new RuntimeException("causa");
        DlmsCommunicationException ex = new DlmsCommunicationException("errore", cause);
        assertEquals("errore", ex.getMessage());
        assertEquals(cause, ex.getCause());
        assertNull(ex.getErrorCode());
    }

    @Test
    void constructorWithMessageAndErrorCode() {
        DlmsCommunicationException ex = new DlmsCommunicationException("errore", 42);
        assertEquals("errore", ex.getMessage());
        assertEquals(42, ex.getErrorCode());
    }

    @Test
    void constructorWithAllParameters() {
        Exception cause = new RuntimeException("causa");
        DlmsCommunicationException ex = new DlmsCommunicationException("errore", cause, 99);
        assertEquals("errore", ex.getMessage());
        assertEquals(cause, ex.getCause());
        assertEquals(99, ex.getErrorCode());
    }

    @Test
    void isRuntimeException() {
        DlmsCommunicationException ex = new DlmsCommunicationException("test");
        assertInstanceOf(RuntimeException.class, ex);
    }
}
