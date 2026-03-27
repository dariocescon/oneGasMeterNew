package com.aton.proj.oneGasMeter.entity;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test unitari per l'entita' DeviceCommand.
 */
class DeviceCommandTest {

    @Test
    void createAndPopulateEntity() {
        DeviceCommand cmd = new DeviceCommand();
        cmd.setSerialNumber("SN001");
        cmd.setCommandType(CommandType.SYNC_CLOCK);
        cmd.setStatus(CommandStatus.PENDING);
        cmd.setCreatedAt(Instant.now());

        assertEquals("SN001", cmd.getSerialNumber());
        assertEquals(CommandType.SYNC_CLOCK, cmd.getCommandType());
        assertEquals(CommandStatus.PENDING, cmd.getStatus());
        assertNotNull(cmd.getCreatedAt());
    }

    @Test
    void defaultStatusIsPending() {
        DeviceCommand cmd = new DeviceCommand();
        assertEquals(CommandStatus.PENDING, cmd.getStatus());
    }

    @Test
    void executedAtAndErrorMessageAreNullByDefault() {
        DeviceCommand cmd = new DeviceCommand();
        assertNull(cmd.getExecutedAt());
        assertNull(cmd.getErrorMessage());
    }

    @Test
    void payloadCanContainJson() {
        DeviceCommand cmd = new DeviceCommand();
        cmd.setPayload("{\"ip\":\"10.0.0.1\",\"port\":4059}");
        assertEquals("{\"ip\":\"10.0.0.1\",\"port\":4059}", cmd.getPayload());
    }

    @Test
    void toStringContainsKeyFields() {
        DeviceCommand cmd = new DeviceCommand();
        cmd.setSerialNumber("SN002");
        cmd.setCommandType(CommandType.DISCONNECT_VALVE);
        cmd.setStatus(CommandStatus.IN_PROGRESS);

        String str = cmd.toString();
        assertTrue(str.contains("SN002"));
        assertTrue(str.contains("DISCONNECT_VALVE"));
        assertTrue(str.contains("IN_PROGRESS"));
    }

    @Test
    void allCommandStatusValues() {
        assertEquals(4, CommandStatus.values().length);
        assertNotNull(CommandStatus.PENDING);
        assertNotNull(CommandStatus.IN_PROGRESS);
        assertNotNull(CommandStatus.DONE);
        assertNotNull(CommandStatus.FAILED);
    }

    @Test
    void allCommandTypeValues() {
        assertTrue(CommandType.values().length >= 5);
        assertNotNull(CommandType.SYNC_CLOCK);
        assertNotNull(CommandType.SET_CLOCK);
        assertNotNull(CommandType.DISCONNECT_VALVE);
        assertNotNull(CommandType.RECONNECT_VALVE);
        assertNotNull(CommandType.CHANGE_PUSH_DESTINATION);
    }
}
