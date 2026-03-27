package com.aton.proj.oneGasMeter.cosem;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test unitari per l'enum CosemObject.
 */
class CosemObjectTest {

    private static final Pattern OBIS_PATTERN = Pattern.compile("\\d+\\.\\d+\\.\\d+\\.\\d+\\.\\d+\\.\\d+");

    @Test
    void allEntriesHaveValidObisCode() {
        for (CosemObject obj : CosemObject.values()) {
            assertTrue(OBIS_PATTERN.matcher(obj.getObisCode()).matches(),
                    obj.name() + " ha un codice OBIS non valido: " + obj.getObisCode());
        }
    }

    @Test
    void allEntriesHavePositiveClassId() {
        for (CosemObject obj : CosemObject.values()) {
            assertTrue(obj.getClassId() > 0,
                    obj.name() + " ha un Class ID non valido: " + obj.getClassId());
        }
    }

    @Test
    void allEntriesHaveDescription() {
        for (CosemObject obj : CosemObject.values()) {
            assertNotNull(obj.getDescription(), obj.name() + " ha descrizione null");
            assertFalse(obj.getDescription().isEmpty(), obj.name() + " ha descrizione vuota");
        }
    }

    @Test
    void findByObisCodeReturnsCorrectObject() {
        Optional<CosemObject> result = CosemObject.findByObisCode("0.0.96.1.0.255");
        assertTrue(result.isPresent());
        assertEquals(CosemObject.SERIAL_NUMBER, result.get());
    }

    @Test
    void findByObisCodeReturnsEmptyForUnknown() {
        Optional<CosemObject> result = CosemObject.findByObisCode("99.99.99.99.99.255");
        assertTrue(result.isEmpty());
    }

    @Test
    void serialNumberIsData() {
        assertEquals(1, CosemObject.SERIAL_NUMBER.getClassId());
    }

    @Test
    void gasVolumeTotalIsRegister() {
        assertEquals(3, CosemObject.GAS_VOLUME_TOTAL.getClassId());
    }

    @Test
    void clockIsClassId8() {
        assertEquals(8, CosemObject.CLOCK.getClassId());
    }

    @Test
    void loadProfileIsProfileGeneric() {
        assertEquals(7, CosemObject.LOAD_PROFILE_1.getClassId());
    }

    @Test
    void isAutoReadableExcludesProfiles() {
        assertFalse(CosemObject.LOAD_PROFILE_1.isAutoReadable());
        assertFalse(CosemObject.EVENT_LOG.isAutoReadable());
    }

    @Test
    void isAutoReadableExcludesSecurityAndAssociation() {
        assertFalse(CosemObject.ASSOCIATION_LN.isAutoReadable());
        assertFalse(CosemObject.SECURITY_SETUP.isAutoReadable());
    }

    @Test
    void isAutoReadableIncludesDataAndRegisters() {
        assertTrue(CosemObject.SERIAL_NUMBER.isAutoReadable());
        assertTrue(CosemObject.GAS_VOLUME_TOTAL.isAutoReadable());
        assertTrue(CosemObject.CLOCK.isAutoReadable());
        assertTrue(CosemObject.VALVE_STATE.isAutoReadable());
    }
}
