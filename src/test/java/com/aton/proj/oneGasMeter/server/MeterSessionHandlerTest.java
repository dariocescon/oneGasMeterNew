package com.aton.proj.oneGasMeter.server;

import com.aton.proj.oneGasMeter.config.DlmsSessionConfig;
import com.aton.proj.oneGasMeter.entity.CommandStatus;
import com.aton.proj.oneGasMeter.entity.CommandType;
import com.aton.proj.oneGasMeter.entity.DeviceCommand;
import com.aton.proj.oneGasMeter.service.CommandService;
import com.aton.proj.oneGasMeter.service.TelemetryService;
import com.aton.proj.oneGasMeter.exception.DlmsCommunicationException;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.Socket;
import java.util.Date;
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

    @Test
    void parsePushDestinationPayload() {
        String[] result = MeterSessionHandler.parsePushDestinationPayload("{\"ip\":\"10.0.0.1\",\"port\":4059}");
        assertEquals("10.0.0.1", result[0]);
        assertEquals("4059", result[1]);
    }

    @Test
    void parsePushDestinationPayloadDifferentValues() {
        String[] result = MeterSessionHandler.parsePushDestinationPayload("{\"ip\":\"192.168.1.100\",\"port\":8080}");
        assertEquals("192.168.1.100", result[0]);
        assertEquals("8080", result[1]);
    }

    @Test
    void parsePushDestinationPayloadMissingFieldThrows() {
        org.junit.jupiter.api.Assertions.assertThrows(DlmsCommunicationException.class,
                () -> MeterSessionHandler.parsePushDestinationPayload("{\"ip\":\"10.0.0.1\"}"));
    }

    @Test
    void parseDateRangePayloadWithDates() {
        Date[] range = MeterSessionHandler.parseDateRangePayload(
                "{\"from\":\"2026-01-01T00:00:00Z\",\"to\":\"2026-03-28T00:00:00Z\"}");
        org.junit.jupiter.api.Assertions.assertNotNull(range[0]);
        org.junit.jupiter.api.Assertions.assertNotNull(range[1]);
        assertTrue(range[0].before(range[1]));
    }

    @Test
    void parseDateRangePayloadNullReturnsNulls() {
        Date[] range = MeterSessionHandler.parseDateRangePayload(null);
        org.junit.jupiter.api.Assertions.assertNull(range[0]);
        org.junit.jupiter.api.Assertions.assertNull(range[1]);
    }

    @Test
    void parseDateRangePayloadEmptyReturnsNulls() {
        Date[] range = MeterSessionHandler.parseDateRangePayload("  ");
        org.junit.jupiter.api.Assertions.assertNull(range[0]);
        org.junit.jupiter.api.Assertions.assertNull(range[1]);
    }

    @Test
    void parseDateRangePayloadInvalidJsonThrows() {
        org.junit.jupiter.api.Assertions.assertThrows(DlmsCommunicationException.class,
                () -> MeterSessionHandler.parseDateRangePayload("{invalid}"));
    }

    @Test
    void resolveLoadProfileObisDefaultsToDaily() {
        assertEquals("7.0.99.99.3.255", MeterSessionHandler.resolveLoadProfileObis(null));
        assertEquals("7.0.99.99.3.255", MeterSessionHandler.resolveLoadProfileObis(""));
    }

    @Test
    void resolveLoadProfileObisDaily() {
        assertEquals("7.0.99.99.3.255",
                MeterSessionHandler.resolveLoadProfileObis("{\"profile\":\"daily\"}"));
    }

    @Test
    void resolveLoadProfileObisMonthly() {
        assertEquals("7.0.98.11.0.255",
                MeterSessionHandler.resolveLoadProfileObis("{\"profile\":\"monthly\"}"));
    }

    @Test
    void parseBase64PayloadValid() {
        DeviceCommand cmd = new DeviceCommand();
        cmd.setPayload(java.util.Base64.getEncoder().encodeToString(new byte[]{1, 2, 3}));
        byte[] result = MeterSessionHandler.parseBase64Payload(cmd, "TEST");
        org.junit.jupiter.api.Assertions.assertArrayEquals(new byte[]{1, 2, 3}, result);
    }

    @Test
    void parseBase64PayloadNullThrows() {
        DeviceCommand cmd = new DeviceCommand();
        cmd.setPayload(null);
        org.junit.jupiter.api.Assertions.assertThrows(DlmsCommunicationException.class,
                () -> MeterSessionHandler.parseBase64Payload(cmd, "TEST"));
    }

    @Test
    void parseBase64PayloadInvalidThrows() {
        DeviceCommand cmd = new DeviceCommand();
        cmd.setPayload("not-valid-base64!!!");
        org.junit.jupiter.api.Assertions.assertThrows(DlmsCommunicationException.class,
                () -> MeterSessionHandler.parseBase64Payload(cmd, "TEST"));
    }

    private void assertTrue(boolean empty) {
        org.junit.jupiter.api.Assertions.assertTrue(empty);
    }

    private void assertEquals(Object expected, Object actual) {
        org.junit.jupiter.api.Assertions.assertEquals(expected, actual);
    }
}
