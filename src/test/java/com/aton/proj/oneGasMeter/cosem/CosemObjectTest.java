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
    void currentIndexConvertedVolIsRegister() {
        assertEquals(3, CosemObject.CURRENT_INDEX_CONVERTED_VOL.getClassId());
    }

    @Test
    void clockIsClassId8() {
        assertEquals(8, CosemObject.CLOCK.getClassId());
    }

    @Test
    void dailyLoadProfileIsProfileGeneric() {
        assertEquals(7, CosemObject.DAILY_LOAD_PROFILE.getClassId());
    }

    @Test
    void isAutoReadableExcludesProfiles() {
        assertFalse(CosemObject.DAILY_LOAD_PROFILE.isAutoReadable());
        assertFalse(CosemObject.METROLOGICAL_LOGBOOK.isAutoReadable());
    }

    @Test
    void isAutoReadableExcludesSecurityAndAssociation() {
        assertFalse(CosemObject.MGMT_ASSOCIATION.isAutoReadable());
        assertFalse(CosemObject.MGMT_SECURITY_SETUP.isAutoReadable());
    }

    @Test
    void isAutoReadableExcludesCompactFrames() {
        assertFalse(CosemObject.CF47_CONTENT_A.isAutoReadable());
        assertFalse(CosemObject.CF49_CONTENT_C.isAutoReadable());
    }

    @Test
    void isAutoReadableExcludesPushAndSchedule() {
        assertFalse(CosemObject.PUSH_SETUP_1.isAutoReadable());
        assertFalse(CosemObject.PUSH_SCHEDULER_1.isAutoReadable());
    }

    @Test
    void isAutoReadableIncludesDataAndRegisters() {
        assertTrue(CosemObject.SERIAL_NUMBER.isAutoReadable());
        assertTrue(CosemObject.CURRENT_INDEX_CONVERTED_VOL.isAutoReadable());
        assertTrue(CosemObject.CLOCK.isAutoReadable());
        assertTrue(CosemObject.VALVE_STATE.isAutoReadable());
    }

    @Test
    void enumContainsExpectedNormativeObjects() {
        // Verifica che tutti gli oggetti chiave della normativa siano presenti
        assertTrue(CosemObject.findByObisCode("0.0.1.1.0.255").isPresent(), "UNIX_TIME mancante");
        assertTrue(CosemObject.findByObisCode("7.0.13.2.0.255").isPresent(), "CURRENT_INDEX_CONVERTED_VOL mancante");
        assertTrue(CosemObject.findByObisCode("7.0.99.99.3.255").isPresent(), "DAILY_LOAD_PROFILE mancante");
        assertTrue(CosemObject.findByObisCode("7.0.99.98.1.255").isPresent(), "METROLOGICAL_LOGBOOK mancante");
        assertTrue(CosemObject.findByObisCode("0.0.66.0.49.255").isPresent(), "CF49_CONTENT_C mancante");
        assertTrue(CosemObject.findByObisCode("0.0.44.0.0.255").isPresent(), "IMAGE_TRANSFER mancante");
    }
}
