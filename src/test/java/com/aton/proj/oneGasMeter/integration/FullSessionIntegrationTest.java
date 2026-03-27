package com.aton.proj.oneGasMeter.integration;

import com.aton.proj.oneGasMeter.entity.CommandStatus;
import com.aton.proj.oneGasMeter.entity.CommandType;
import com.aton.proj.oneGasMeter.entity.DeviceCommand;
import com.aton.proj.oneGasMeter.repository.DeviceCommandRepository;
import com.aton.proj.oneGasMeter.repository.TelemetryDataRepository;
import com.aton.proj.oneGasMeter.service.CommandService;
import com.aton.proj.oneGasMeter.service.TelemetryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test di integrazione end-to-end con H2.
 * Verifica che i servizi interagiscano correttamente con il database.
 */
@SpringBootTest
class FullSessionIntegrationTest {

    @Autowired
    private TelemetryService telemetryService;

    @Autowired
    private CommandService commandService;

    @Autowired
    private TelemetryDataRepository telemetryRepository;

    @Autowired
    private DeviceCommandRepository commandRepository;

    @Test
    void simulateFullSessionWithoutDlms() {
        String serialNumber = "INTEG-SN001";
        String sessionId = "integ-session-001";
        String meterIp = "192.168.1.100";
        Instant meterTimestamp = Instant.now();

        // 1. Inserisci un comando pendente per il dispositivo
        DeviceCommand pendingCmd = new DeviceCommand();
        pendingCmd.setSerialNumber(serialNumber);
        pendingCmd.setCommandType(CommandType.SYNC_CLOCK);
        pendingCmd.setStatus(CommandStatus.PENDING);
        pendingCmd.setCreatedAt(Instant.now());
        commandRepository.save(pendingCmd);

        // 2. Simula il salvataggio di letture di telemetria
        telemetryService.save(serialNumber, meterIp, sessionId,
                "0.0.96.1.0.255", 1, serialNumber, 0, null, meterTimestamp);

        telemetryService.save(serialNumber, meterIp, sessionId,
                "7.0.13.2.0.255", 3, "12345", -3, "m3", meterTimestamp);

        telemetryService.save(serialNumber, meterIp, sessionId,
                "7.0.42.0.0.255", 3, "1013", -1, "mbar", meterTimestamp);

        // 3. Verifica che le letture siano state salvate
        var telemetryData = telemetryRepository.findBySessionId(sessionId);
        assertEquals(3, telemetryData.size());
        assertTrue(telemetryData.stream().allMatch(t -> t.getSerialNumber().equals(serialNumber)));

        // 4. Verifica il valore scalato del volume gas
        var volumeReading = telemetryData.stream()
                .filter(t -> t.getObisCode().equals("7.0.13.2.0.255"))
                .findFirst()
                .orElseThrow();
        assertNotNull(volumeReading.getScaledValue());
        assertEquals(12.345, volumeReading.getScaledValue(), 0.001);

        // 5. Simula l'esecuzione del comando pendente
        List<DeviceCommand> pendingCommands = commandService.getPendingCommands(serialNumber);
        assertEquals(1, pendingCommands.size());

        DeviceCommand cmd = pendingCommands.get(0);
        commandService.markInProgress(cmd);
        commandService.markDone(cmd);

        // 6. Verifica che non ci siano piu' comandi pendenti
        List<DeviceCommand> remaining = commandService.getPendingCommands(serialNumber);
        assertTrue(remaining.isEmpty());

        // 7. Verifica che il comando sia stato marcato come DONE
        DeviceCommand executedCmd = commandRepository.findById(cmd.getId()).orElseThrow();
        assertEquals(CommandStatus.DONE, executedCmd.getStatus());
        assertNotNull(executedCmd.getExecutedAt());
    }

    @Test
    void commandFailureIsPersisted() {
        String serialNumber = "INTEG-SN002";

        DeviceCommand cmd = new DeviceCommand();
        cmd.setSerialNumber(serialNumber);
        cmd.setCommandType(CommandType.DISCONNECT_VALVE);
        cmd.setStatus(CommandStatus.PENDING);
        cmd.setCreatedAt(Instant.now());
        commandRepository.save(cmd);

        List<DeviceCommand> pending = commandService.getPendingCommands(serialNumber);
        assertEquals(1, pending.size());

        // Simula un fallimento
        commandService.markInProgress(pending.get(0));
        commandService.markFailed(pending.get(0), "Timeout comunicazione con il contatore");

        DeviceCommand failedCmd = commandRepository.findById(pending.get(0).getId()).orElseThrow();
        assertEquals(CommandStatus.FAILED, failedCmd.getStatus());
        assertEquals("Timeout comunicazione con il contatore", failedCmd.getErrorMessage());
        assertNotNull(failedCmd.getExecutedAt());
    }
}
