package com.aton.proj.oneGasMeter.cosem;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test unitari per EventCode.
 */
class EventCodeTest {

    @Test
    void findByCodeReturnsCorrectEvent() {
        EventCode ec = EventCode.findByCode(1);
        assertNotNull(ec);
        assertEquals(EventCode.DEVICE_RESET, ec);
        assertTrue(ec.isMetrological());
    }

    @Test
    void findByCodeReturnsNullForUnknown() {
        assertNull(EventCode.findByCode(999));
    }

    @Test
    void findByCodeReturnsNullForReservedRange() {
        assertNull(EventCode.findByCode(143)); // riservato
    }

    @Test
    void metrologicalEventsAreMarked() {
        assertTrue(EventCode.DEVICE_RESET.isMetrological());
        assertTrue(EventCode.VALVE_CLOSED_REMOTE.isMetrological());
        assertTrue(EventCode.TAMPER_DETECTED_START.isMetrological());
        assertTrue(EventCode.FW_VERIFY_FAILED.isMetrological());
    }

    @Test
    void nonMetrologicalEventsAreMarked() {
        assertFalse(EventCode.NEW_TARIFF_ACTIVATED.isMetrological());
        assertFalse(EventCode.LOCAL_SESSION_START.isMetrological());
        assertFalse(EventCode.PUSH_SETUP_1_MODIFIED.isMetrological());
    }

    @Test
    void describeCodeKnownEvent() {
        String desc = EventCode.describeCode(30);
        assertEquals("Valvola chiusa per comando remoto/locale", desc);
    }

    @Test
    void describeCodeManufacturerEvent() {
        String desc = EventCode.describeCode(200);
        assertTrue(desc.contains("fabbricante"));
    }

    @Test
    void describeCodeUnknownEvent() {
        String desc = EventCode.describeCode(143);
        assertTrue(desc.contains("sconosciuto"));
    }

    @Test
    void allEventsHaveUniqueCode() {
        EventCode[] all = EventCode.values();
        for (int i = 0; i < all.length; i++) {
            for (int j = i + 1; j < all.length; j++) {
                assertNotEquals(all[i].getCode(), all[j].getCode(),
                        all[i].name() + " e " + all[j].name() + " hanno lo stesso codice");
            }
        }
    }

    @Test
    void allEventsHaveDescription() {
        for (EventCode ec : EventCode.values()) {
            assertNotNull(ec.getDescription());
            assertFalse(ec.getDescription().isEmpty());
        }
    }

    @Test
    void lastEventIs181() {
        EventCode ec = EventCode.findByCode(181);
        assertNotNull(ec);
        assertEquals(EventCode.IM_ASSOCIATION_DISABLED, ec);
    }
}
