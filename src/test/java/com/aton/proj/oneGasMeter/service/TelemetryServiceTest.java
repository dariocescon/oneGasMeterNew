package com.aton.proj.oneGasMeter.service;

import com.aton.proj.oneGasMeter.entity.TelemetryData;
import com.aton.proj.oneGasMeter.repository.TelemetryDataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test unitari per TelemetryService.
 */
class TelemetryServiceTest {

    private TelemetryDataRepository repository;
    private TelemetryService service;

    @BeforeEach
    void setUp() {
        repository = mock(TelemetryDataRepository.class);
        service = new TelemetryService(repository);

        // Il mock restituisce l'entita' passata
        when(repository.save(any(TelemetryData.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void saveCreatesEntityWithCorrectFields() {
        Instant timestamp = Instant.now();

        service.save("SN001", "10.0.0.1", "session-1",
                "0.0.96.1.0.255", 1, "test-value", 0, null, timestamp);

        ArgumentCaptor<TelemetryData> captor = ArgumentCaptor.forClass(TelemetryData.class);
        verify(repository).save(captor.capture());

        TelemetryData saved = captor.getValue();
        assertEquals("SN001", saved.getSerialNumber());
        assertEquals("10.0.0.1", saved.getMeterIp());
        assertEquals("session-1", saved.getSessionId());
        assertEquals("0.0.96.1.0.255", saved.getObisCode());
        assertEquals(1, saved.getClassId());
        assertEquals("test-value", saved.getRawValue());
        assertEquals(timestamp, saved.getMeterTimestamp());
        assertNotNull(saved.getReceivedAt());
    }

    @Test
    void saveComputesScaledValueForNumericData() {
        service.save("SN001", "10.0.0.1", "session-1",
                "7.0.13.2.0.255", 3, "12345", -3, "m3", Instant.now());

        ArgumentCaptor<TelemetryData> captor = ArgumentCaptor.forClass(TelemetryData.class);
        verify(repository).save(captor.capture());

        TelemetryData saved = captor.getValue();
        assertNotNull(saved.getScaledValue());
        assertEquals(12.345, saved.getScaledValue(), 0.001);
    }

    @Test
    void saveReturnsNullScaledValueForNonNumericData() {
        service.save("SN001", "10.0.0.1", "session-1",
                "0.0.96.1.0.255", 1, "non-numeric", 0, null, Instant.now());

        ArgumentCaptor<TelemetryData> captor = ArgumentCaptor.forClass(TelemetryData.class);
        verify(repository).save(captor.capture());

        assertNull(captor.getValue().getScaledValue());
    }

    @Test
    void saveHandlesNullRawValue() {
        service.save("SN001", "10.0.0.1", "session-1",
                "0.0.96.1.0.255", 1, null, 0, null, Instant.now());

        ArgumentCaptor<TelemetryData> captor = ArgumentCaptor.forClass(TelemetryData.class);
        verify(repository).save(captor.capture());

        assertNull(captor.getValue().getRawValue());
        assertNull(captor.getValue().getScaledValue());
    }

    @Test
    void saveWithZeroScaler() {
        service.save("SN001", "10.0.0.1", "session-1",
                "7.0.13.2.0.255", 3, "100", 0, "m3", Instant.now());

        ArgumentCaptor<TelemetryData> captor = ArgumentCaptor.forClass(TelemetryData.class);
        verify(repository).save(captor.capture());

        // 100 * 10^0 = 100
        assertEquals(100.0, captor.getValue().getScaledValue(), 0.001);
    }
}
