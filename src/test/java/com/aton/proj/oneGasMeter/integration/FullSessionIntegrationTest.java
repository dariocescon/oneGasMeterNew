package com.aton.proj.oneGasMeter.integration;

import com.aton.proj.oneGasMeter.cosem.CompactFrameData;
import com.aton.proj.oneGasMeter.cosem.CompactFrameParser;
import com.aton.proj.oneGasMeter.cosem.EventCode;
import com.aton.proj.oneGasMeter.cosem.ValveStatus;
import com.aton.proj.oneGasMeter.entity.CommandStatus;
import com.aton.proj.oneGasMeter.entity.CommandType;
import com.aton.proj.oneGasMeter.entity.DeviceCommand;
import com.aton.proj.oneGasMeter.entity.DeviceRegistry;
import com.aton.proj.oneGasMeter.repository.DeviceCommandRepository;
import com.aton.proj.oneGasMeter.repository.DeviceRegistryRepository;
import com.aton.proj.oneGasMeter.repository.TelemetryDataRepository;
import com.aton.proj.oneGasMeter.service.CommandService;
import com.aton.proj.oneGasMeter.service.KeyEncryptionService;
import com.aton.proj.oneGasMeter.service.TelemetryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test di integrazione end-to-end con H2.
 * Verifica che servizi, repository, parser e cifratura chiavi
 * funzionino insieme correttamente.
 */
@SpringBootTest
class FullSessionIntegrationTest {

    @Autowired
    private TelemetryService telemetryService;

    @Autowired
    private CommandService commandService;

    @Autowired
    private KeyEncryptionService keyEncryptionService;

    @Autowired
    private TelemetryDataRepository telemetryRepository;

    @Autowired
    private DeviceCommandRepository commandRepository;

    @Autowired
    private DeviceRegistryRepository deviceRegistryRepository;

    @Test
    void simulateFullSessionWithoutDlms() {
        String serialNumber = "INTEG-SN001";
        String sessionId = "integ-session-001";
        String meterIp = "192.168.1.100";
        Instant meterTimestamp = Instant.now();

        // 1. Inserisci un comando pendente
        DeviceCommand pendingCmd = new DeviceCommand();
        pendingCmd.setSerialNumber(serialNumber);
        pendingCmd.setCommandType(CommandType.SYNC_CLOCK);
        pendingCmd.setStatus(CommandStatus.PENDING);
        pendingCmd.setCreatedAt(Instant.now());
        commandRepository.save(pendingCmd);

        // 2. Simula salvataggio letture
        telemetryService.save(serialNumber, meterIp, sessionId,
                "0.0.96.1.0.255", 1, serialNumber, 0, null, meterTimestamp);
        telemetryService.save(serialNumber, meterIp, sessionId,
                "7.0.13.2.0.255", 3, "12345", -3, "m3", meterTimestamp);
        telemetryService.save(serialNumber, meterIp, sessionId,
                "7.0.12.2.0.255", 3, "100", -3, "m3", meterTimestamp);

        // 3. Verifica le letture salvate
        var telemetryData = telemetryRepository.findBySessionId(sessionId);
        assertEquals(3, telemetryData.size());

        // 4. Verifica valore scalato volume gas
        var volumeReading = telemetryData.stream()
                .filter(t -> t.getObisCode().equals("7.0.13.2.0.255"))
                .findFirst().orElseThrow();
        assertNotNull(volumeReading.getScaledValue());
        assertEquals(12.345, volumeReading.getScaledValue(), 0.001);

        // 5. Esegui comando pendente
        List<DeviceCommand> pendingCommands = commandService.getPendingCommands(serialNumber);
        assertEquals(1, pendingCommands.size());
        commandService.markInProgress(pendingCommands.get(0));
        commandService.markDone(pendingCommands.get(0));

        // 6. Verifica che il comando sia DONE
        assertTrue(commandService.getPendingCommands(serialNumber).isEmpty());
        DeviceCommand executedCmd = commandRepository.findById(pendingCmd.getId()).orElseThrow();
        assertEquals(CommandStatus.DONE, executedCmd.getStatus());
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

        commandService.markInProgress(cmd);
        commandService.markFailed(cmd, "Timeout comunicazione con il contatore");

        DeviceCommand failedCmd = commandRepository.findById(cmd.getId()).orElseThrow();
        assertEquals(CommandStatus.FAILED, failedCmd.getStatus());
        assertEquals("Timeout comunicazione con il contatore", failedCmd.getErrorMessage());
    }

    @Test
    void deviceRegistryWithEncryptedKeys() {
        String serial = "INTEG-SN003";
        byte[] plainKey = {0x00, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77,
                           (byte)0x88, (byte)0x99, (byte)0xAA, (byte)0xBB,
                           (byte)0xCC, (byte)0xDD, (byte)0xEE, (byte)0xFF};

        // 1. Cifra la chiave e salva il dispositivo
        byte[] encryptedKey = keyEncryptionService.encrypt(plainKey);

        DeviceRegistry device = new DeviceRegistry();
        device.setSerialNumber(serial);
        device.setDeviceType("RSE");
        device.setEncryptionKeyEnc(encryptedKey);
        device.setAuthenticationKeyEnc(keyEncryptionService.encrypt(plainKey));
        device.setSystemTitle(new byte[]{0x53, 0x41, 0x43, 0x53, 0x41, 0x43, 0x53, 0x41});
        deviceRegistryRepository.save(device);

        // 2. Recupera e decifra
        DeviceRegistry loaded = deviceRegistryRepository.findById(serial).orElseThrow();
        byte[] decryptedKey = keyEncryptionService.decrypt(loaded.getEncryptionKeyEnc());

        assertArrayEquals(plainKey, decryptedKey);
        assertEquals("RSE", loaded.getDeviceType());
    }

    @Test
    void compactFrameParsingAndTelemetrySave() {
        String serial = "INTEG-SN004";
        String sessionId = "integ-session-004";
        String meterIp = "10.0.0.1";

        // 1. Costruisci una CF47 simulata
        long epoch = 1700000000L;
        ByteBuffer bb = ByteBuffer.allocate(29).order(ByteOrder.BIG_ENDIAN);
        bb.put((byte) 47);           // template_id
        bb.putInt((int) epoch);       // UNIX time
        bb.putShort((short) 1);       // PP4 Network Status
        bb.put((byte) 1);            // valve open
        bb.put((byte) 1);            // valve connected
        bb.putShort((short) 5);       // metrological event counter
        bb.putShort((short) 12);      // event counter
        bb.putShort((short) 0);       // daily diagnostic
        bb.putInt(500000);            // converted volume = 500.000 (scaler -3)
        bb.putInt(0);                 // converted volume alarm
        bb.put((byte) 3);            // billing period counter
        bb.putInt(42);                // mgmt frame counter

        // 2. Parsa la compact frame
        CompactFrameData cfData = CompactFrameParser.parse(bb.array());
        assertNotNull(cfData);
        assertEquals(47, cfData.getTemplateId());
        assertEquals(Instant.ofEpochSecond(epoch), cfData.getTimestamp());

        // 3. Salva i dati in telemetry
        Instant timestamp = cfData.getTimestamp();
        for (var entry : cfData.getValues().entrySet()) {
            if (entry.getValue() instanceof byte[]) continue;
            telemetryService.save(serial, meterIp, sessionId,
                    entry.getKey(), 62, entry.getValue(), 0, null, timestamp);
        }

        // 4. Verifica il salvataggio
        var saved = telemetryRepository.findBySessionId(sessionId);
        assertFalse(saved.isEmpty());
        assertTrue(saved.stream().allMatch(t -> t.getSerialNumber().equals(serial)));
    }

    @Test
    void valveStatusFromCF8() {
        byte[] cf8Buf = new byte[]{8, 0, 2, 107}; // closed, ready_for_reconnection, cause=107
        CompactFrameData cfData = CompactFrameParser.parse(cf8Buf);
        ValveStatus vs = ValveStatus.fromCompactFrame(cfData);

        assertNotNull(vs);
        assertTrue(vs.isClosed());
        assertEquals(2, vs.getControlState());
        assertEquals("ready_for_reconnection", vs.getControlStateDescription());
        // Cause 107 = "Valvola chiusa per nessuna comunicazione"
        assertEquals(107, vs.getClosureCause());
    }

    @Test
    void eventCodeLookup() {
        // Verifica che i codici evento principali siano accessibili
        EventCode tamper = EventCode.findByCode(80);
        assertNotNull(tamper);
        assertTrue(tamper.isMetrological());

        EventCode fwOk = EventCode.findByCode(100);
        assertNotNull(fwOk);
        assertEquals("Aggiornamento firmware: attivazione con successo", fwOk.getDescription());

        // Codici fabbricante
        String desc = EventCode.describeCode(200);
        assertTrue(desc.contains("fabbricante"));
    }

    @Test
    void allCommandTypesExist() {
        // Verifica che tutti i 28 tipi di comando siano definiti
        assertEquals(28, CommandType.values().length);
    }
}
