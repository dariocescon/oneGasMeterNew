package com.aton.proj.oneGasMeter.repository;

import com.aton.proj.oneGasMeter.entity.CommandStatus;
import com.aton.proj.oneGasMeter.entity.CommandType;
import com.aton.proj.oneGasMeter.entity.DeviceCommand;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test di integrazione per DeviceCommandRepository con H2.
 */
@SpringBootTest
@Transactional
class DeviceCommandRepositoryTest {

    @Autowired
    private DeviceCommandRepository repository;

    @Test
    void saveAndFindBySerialNumberAndStatus() {
        repository.save(createCommand("SN001", CommandType.SYNC_CLOCK, CommandStatus.PENDING));
        repository.save(createCommand("SN001", CommandType.DISCONNECT_VALVE, CommandStatus.DONE));
        repository.save(createCommand("SN002", CommandType.SET_CLOCK, CommandStatus.PENDING));

        List<DeviceCommand> result = repository.findBySerialNumberAndStatus("SN001", CommandStatus.PENDING);
        assertEquals(1, result.size());
        assertEquals(CommandType.SYNC_CLOCK, result.get(0).getCommandType());
    }

    @Test
    void findBySerialNumber() {
        repository.save(createCommand("SN001", CommandType.SYNC_CLOCK, CommandStatus.PENDING));
        repository.save(createCommand("SN001", CommandType.DISCONNECT_VALVE, CommandStatus.DONE));

        List<DeviceCommand> result = repository.findBySerialNumber("SN001");
        assertEquals(2, result.size());
    }

    @Test
    void emptyResultForNoPendingCommands() {
        repository.save(createCommand("SN001", CommandType.SYNC_CLOCK, CommandStatus.DONE));

        List<DeviceCommand> result = repository.findBySerialNumberAndStatus("SN001", CommandStatus.PENDING);
        assertTrue(result.isEmpty());
    }

    @Test
    void statusTransitionIsPersisted() {
        DeviceCommand cmd = createCommand("SN001", CommandType.RECONNECT_VALVE, CommandStatus.PENDING);
        cmd = repository.save(cmd);

        cmd.setStatus(CommandStatus.IN_PROGRESS);
        repository.save(cmd);

        cmd.setStatus(CommandStatus.DONE);
        cmd.setExecutedAt(Instant.now());
        repository.save(cmd);

        DeviceCommand found = repository.findById(cmd.getId()).orElse(null);
        assertNotNull(found);
        assertEquals(CommandStatus.DONE, found.getStatus());
        assertNotNull(found.getExecutedAt());
    }

    private DeviceCommand createCommand(String serial, CommandType type, CommandStatus status) {
        DeviceCommand cmd = new DeviceCommand();
        cmd.setSerialNumber(serial);
        cmd.setCommandType(type);
        cmd.setStatus(status);
        cmd.setCreatedAt(Instant.now());
        return cmd;
    }
}
