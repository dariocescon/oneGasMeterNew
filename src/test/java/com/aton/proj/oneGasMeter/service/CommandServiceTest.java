package com.aton.proj.oneGasMeter.service;

import com.aton.proj.oneGasMeter.entity.CommandStatus;
import com.aton.proj.oneGasMeter.entity.CommandType;
import com.aton.proj.oneGasMeter.entity.DeviceCommand;
import com.aton.proj.oneGasMeter.repository.DeviceCommandRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test unitari per CommandService.
 */
class CommandServiceTest {

    private DeviceCommandRepository repository;
    private CommandService service;

    @BeforeEach
    void setUp() {
        repository = mock(DeviceCommandRepository.class);
        service = new CommandService(repository);
        when(repository.save(any(DeviceCommand.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void getPendingCommandsQueriesRepository() {
        DeviceCommand cmd = createCommand("SN001", CommandType.SYNC_CLOCK);
        when(repository.findBySerialNumberAndStatus("SN001", CommandStatus.PENDING))
                .thenReturn(List.of(cmd));

        List<DeviceCommand> result = service.getPendingCommands("SN001");
        assertEquals(1, result.size());
        assertEquals(CommandType.SYNC_CLOCK, result.get(0).getCommandType());
    }

    @Test
    void getPendingCommandsReturnsEmptyListWhenNoPending() {
        when(repository.findBySerialNumberAndStatus("SN002", CommandStatus.PENDING))
                .thenReturn(List.of());

        List<DeviceCommand> result = service.getPendingCommands("SN002");
        assertTrue(result.isEmpty());
    }

    @Test
    void markInProgressUpdatesStatus() {
        DeviceCommand cmd = createCommand("SN001", CommandType.DISCONNECT_VALVE);

        service.markInProgress(cmd);

        assertEquals(CommandStatus.IN_PROGRESS, cmd.getStatus());
        verify(repository).save(cmd);
    }

    @Test
    void markDoneUpdatesStatusAndTimestamp() {
        DeviceCommand cmd = createCommand("SN001", CommandType.RECONNECT_VALVE);

        service.markDone(cmd);

        assertEquals(CommandStatus.DONE, cmd.getStatus());
        assertNotNull(cmd.getExecutedAt());
        verify(repository).save(cmd);
    }

    @Test
    void markFailedUpdatesStatusTimestampAndError() {
        DeviceCommand cmd = createCommand("SN001", CommandType.SET_CLOCK);

        service.markFailed(cmd, "Timeout comunicazione");

        assertEquals(CommandStatus.FAILED, cmd.getStatus());
        assertNotNull(cmd.getExecutedAt());
        assertEquals("Timeout comunicazione", cmd.getErrorMessage());
        verify(repository).save(cmd);
    }

    private DeviceCommand createCommand(String serialNumber, CommandType type) {
        DeviceCommand cmd = new DeviceCommand();
        cmd.setSerialNumber(serialNumber);
        cmd.setCommandType(type);
        cmd.setStatus(CommandStatus.PENDING);
        return cmd;
    }
}
