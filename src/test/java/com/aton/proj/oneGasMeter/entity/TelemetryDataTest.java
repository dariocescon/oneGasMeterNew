package com.aton.proj.oneGasMeter.entity;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test unitari per l'entita' TelemetryData.
 */
class TelemetryDataTest {

    @Test
    void createAndPopulateEntity() {
        TelemetryData data = new TelemetryData();
        data.setSerialNumber("ABC123");
        data.setMeterIp("192.168.1.100");
        data.setObisCode("0.0.96.1.0.255");
        data.setClassId(1);
        data.setRawValue("12345");
        data.setScaler(1.0);
        data.setUnit("m3");
        data.setScaledValue(123.45);
        data.setMeterTimestamp(Instant.now());
        data.setReceivedAt(Instant.now());
        data.setSessionId("session-001");

        assertEquals("ABC123", data.getSerialNumber());
        assertEquals("192.168.1.100", data.getMeterIp());
        assertEquals("0.0.96.1.0.255", data.getObisCode());
        assertEquals(1, data.getClassId());
        assertEquals("12345", data.getRawValue());
        assertEquals(1.0, data.getScaler());
        assertEquals("m3", data.getUnit());
        assertEquals(123.45, data.getScaledValue());
        assertNotNull(data.getMeterTimestamp());
        assertNotNull(data.getReceivedAt());
        assertEquals("session-001", data.getSessionId());
    }

    @Test
    void defaultScalerIsOne() {
        TelemetryData data = new TelemetryData();
        assertEquals(1.0, data.getScaler());
    }

    @Test
    void scaledValueCanBeNull() {
        TelemetryData data = new TelemetryData();
        assertNull(data.getScaledValue());
    }

    @Test
    void toStringContainsKeyFields() {
        TelemetryData data = new TelemetryData();
        data.setSerialNumber("SN001");
        data.setObisCode("0.0.1.0.0.255");
        data.setRawValue("100");

        String str = data.toString();
        assertTrue(str.contains("SN001"));
        assertTrue(str.contains("0.0.1.0.0.255"));
    }
}
