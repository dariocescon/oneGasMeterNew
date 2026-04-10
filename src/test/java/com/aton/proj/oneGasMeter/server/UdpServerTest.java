package com.aton.proj.oneGasMeter.server;

import com.aton.proj.oneGasMeter.config.UdpServerConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test unitari per UdpServer.
 */
class UdpServerTest {

    @Test
    void serverDoesNotStartWhenPortIsZero() throws Exception {
        UdpServerConfig config = new UdpServerConfig();
        config.setPort(0);

        UdpServer server = new UdpServer(config);
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
    }
}
