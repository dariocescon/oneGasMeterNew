package com.aton.proj.oneGasMeter.cosem;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test unitari per ValveStatus.
 */
class ValveStatusTest {

    @Test
    void openValveStatus() {
        ValveStatus vs = new ValveStatus(true, 1, 0);
        assertTrue(vs.isOpen());
        assertFalse(vs.isClosed());
        assertEquals("connected", vs.getControlStateDescription());
        assertEquals("nessuna", vs.getClosureCauseDescription());
    }

    @Test
    void closedValveWithCause() {
        ValveStatus vs = new ValveStatus(false, 0, 107);
        assertTrue(vs.isClosed());
        assertFalse(vs.isOpen());
        assertEquals("disconnected", vs.getControlStateDescription());
        // Codice 107 = "Valvola chiusa per nessuna comunicazione"
        assertTrue(vs.getClosureCauseDescription().contains("comunicazione"));
    }

    @Test
    void readyForReconnection() {
        ValveStatus vs = new ValveStatus(false, 2, 30);
        assertTrue(vs.isClosed());
        assertEquals("ready_for_reconnection", vs.getControlStateDescription());
    }

    @Test
    void fromCompactFrameCF8() {
        byte[] buf = new byte[]{8, 0, 2, 102}; // closed, ready_for_reconnection, cause=102
        CompactFrameData cfData = CompactFrameParser.parse(buf);
        ValveStatus vs = ValveStatus.fromCompactFrame(cfData);

        assertNotNull(vs);
        assertTrue(vs.isClosed());
        assertEquals(2, vs.getControlState());
        assertEquals(102, vs.getClosureCause());
    }

    @Test
    void fromCompactFrameReturnsNullForWrongTemplate() {
        CompactFrameData cfData = new CompactFrameData(47);
        assertNull(ValveStatus.fromCompactFrame(cfData));
    }

    @Test
    void fromCompactFrameReturnsNullForNull() {
        assertNull(ValveStatus.fromCompactFrame(null));
    }

    @Test
    void toStringContainsState() {
        ValveStatus open = new ValveStatus(true, 1, 0);
        assertTrue(open.toString().contains("APERTA"));

        ValveStatus closed = new ValveStatus(false, 0, 30);
        assertTrue(closed.toString().contains("CHIUSA"));
    }
}
