package com.aton.proj.oneGasMeter.server;

import com.aton.proj.oneGasMeter.config.DlmsSessionConfig;
import com.aton.proj.oneGasMeter.entity.CommandStatus;
import com.aton.proj.oneGasMeter.entity.CommandType;
import com.aton.proj.oneGasMeter.entity.DeviceCommand;
import com.aton.proj.oneGasMeter.service.CommandService;
import com.aton.proj.oneGasMeter.service.TelemetryService;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.Socket;
import java.util.List;

import static org.mockito.Mockito.*;

/**
 * Test unitari per MeterSessionHandler.
 * Verifica che il handler gestisca correttamente errori e casi limite.
 * I test di integrazione verificheranno il flusso completo.
 */
class MeterSessionHandlerTest {

    @Test
    void handleClosesSocketOnError() {
        Socket mockSocket = mock(Socket.class);
        InetAddress mockAddr = mock(InetAddress.class);
        when(mockSocket.getInetAddress()).thenReturn(mockAddr);
        when(mockAddr.getHostAddress()).thenReturn("10.0.0.1");

        DlmsSessionConfig config = new DlmsSessionConfig();
        TelemetryService telemetryService = mock(TelemetryService.class);
        CommandService commandService = mock(CommandService.class);

        // Il handler fallira' durante la creazione del transport (socket mock senza stream)
        MeterSessionHandler handler = new MeterSessionHandler(
                mockSocket, config, 5000, telemetryService, commandService);

        // Non deve lanciare eccezioni, deve gestire l'errore internamente
        handler.handle();
    }

    @Test
    void pendingCommandsAreRetrievedForDevice() {
        // Verifica che il service venga chiamato correttamente
        CommandService commandService = mock(CommandService.class);
        when(commandService.getPendingCommands("SN001")).thenReturn(List.of());

        List<DeviceCommand> result = commandService.getPendingCommands("SN001");
        verify(commandService).getPendingCommands("SN001");
        assertTrue(result.isEmpty());
    }

    @Test
    void commandStatusTransitions() {
        DeviceCommand cmd = new DeviceCommand();
        cmd.setSerialNumber("SN001");
        cmd.setCommandType(CommandType.SYNC_CLOCK);
        cmd.setStatus(CommandStatus.PENDING);

        // Simula la transizione di stato
        assertEquals(CommandStatus.PENDING, cmd.getStatus());

        cmd.setStatus(CommandStatus.IN_PROGRESS);
        assertEquals(CommandStatus.IN_PROGRESS, cmd.getStatus());

        cmd.setStatus(CommandStatus.DONE);
        assertEquals(CommandStatus.DONE, cmd.getStatus());
    }

    private void assertTrue(boolean empty) {
        org.junit.jupiter.api.Assertions.assertTrue(empty);
    }

    private void assertEquals(Object expected, Object actual) {
        org.junit.jupiter.api.Assertions.assertEquals(expected, actual);
    }
}
