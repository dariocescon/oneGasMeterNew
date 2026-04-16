package com.aton.proj.oneGasMeter.server;

import com.aton.proj.oneGasMeter.config.DlmsSessionConfig;
import com.aton.proj.oneGasMeter.config.UdpServerConfig;
import com.aton.proj.oneGasMeter.service.TelemetryService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * Test unitari per UdpServer.
 */
class UdpServerTest {

    @Test
    void serverDoesNotStartWhenPortIsZero() throws Exception {
        UdpServerConfig config = new UdpServerConfig();
        config.setPort(0);

        UdpServer server = new UdpServer(config, new DlmsSessionConfig(), mock(TelemetryService.class));
        server.run();

        assertFalse(server.isRunning());
    }

    @Test
    void configDefaultValues() {
        UdpServerConfig config = new UdpServerConfig();
        assertEquals(60104, config.getPort());
        assertEquals(2048, config.getMaxPacketSize());
        assertEquals(3, config.getRetryCount());
        assertEquals(1000, config.getRetryDelayMs());
        assertEquals(30000, config.getSessionTimeoutMs());
    }
}
