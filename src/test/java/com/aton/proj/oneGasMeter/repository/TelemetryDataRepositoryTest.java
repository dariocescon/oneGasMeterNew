package com.aton.proj.oneGasMeter.repository;

import com.aton.proj.oneGasMeter.entity.TelemetryData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test di integrazione per TelemetryDataRepository con H2.
 */
@SpringBootTest
@Transactional
class TelemetryDataRepositoryTest {

    @Autowired
    private TelemetryDataRepository repository;

    @Test
    void saveAndFindBySerialNumber() {
        TelemetryData data = createTelemetryData("SN001", "sess-1", "0.0.96.1.0.255");
        repository.save(data);

        List<TelemetryData> result = repository.findBySerialNumber("SN001");
        assertEquals(1, result.size());
        assertEquals("SN001", result.get(0).getSerialNumber());
    }

    @Test
    void findBySessionId() {
        repository.save(createTelemetryData("SN001", "sess-1", "0.0.96.1.0.255"));
        repository.save(createTelemetryData("SN001", "sess-1", "7.0.13.2.0.255"));
        repository.save(createTelemetryData("SN002", "sess-2", "0.0.96.1.0.255"));

        List<TelemetryData> result = repository.findBySessionId("sess-1");
        assertEquals(2, result.size());
    }

    @Test
    void findBySerialNumberAndObisCode() {
        repository.save(createTelemetryData("SN001", "sess-1", "0.0.96.1.0.255"));
        repository.save(createTelemetryData("SN001", "sess-1", "7.0.13.2.0.255"));

        List<TelemetryData> result = repository.findBySerialNumberAndObisCode("SN001", "7.0.13.2.0.255");
        assertEquals(1, result.size());
        assertEquals("7.0.13.2.0.255", result.get(0).getObisCode());
    }

    @Test
    void emptyResultForUnknownSerialNumber() {
        List<TelemetryData> result = repository.findBySerialNumber("UNKNOWN");
        assertTrue(result.isEmpty());
    }

    private TelemetryData createTelemetryData(String serial, String session, String obis) {
        TelemetryData data = new TelemetryData();
        data.setSerialNumber(serial);
        data.setMeterIp("10.0.0.1");
        data.setSessionId(session);
        data.setObisCode(obis);
        data.setClassId(1);
        data.setRawValue("test");
        data.setScaler(0);
        data.setReceivedAt(Instant.now());
        return data;
    }
}
