package com.aton.proj.oneGasMeter.server;

import com.aton.proj.oneGasMeter.config.UdpServerConfig;
import com.aton.proj.oneGasMeter.cosem.CosemObject;
import com.aton.proj.oneGasMeter.entity.TelemetryData;
import com.aton.proj.oneGasMeter.service.TelemetryService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test unitari per UdpServer.
 */
class UdpServerTest {

    // -------------------------------------------------------
    // Helpers per costruire pacchetti WRAPPER / DATA-NOTIFICATION
    // -------------------------------------------------------

    /** Costruisce un WRAPPER header (8 byte) per il payload dato. */
    private byte[] wrapPacket(byte[] payload) {
        ByteBuffer buf = ByteBuffer.allocate(8 + payload.length).order(ByteOrder.BIG_ENDIAN);
        buf.putShort((short) 0x0001);   // version
        buf.putShort((short) 0x0001);   // source wPort
        buf.putShort((short) 0x0001);   // dest wPort
        buf.putShort((short) payload.length);
        buf.put(payload);
        return buf.array();
    }

    /**
     * Costruisce un APDU DATA-NOTIFICATION (0x0F) con invoke-id fisso
     * e un compact frame buffer passato come octet-string (tag 0x09).
     * Il compact frame e' racchiuso in una struttura (0x02) con 1 elemento.
     */
    private byte[] buildDataNotification(byte[] compactFrameBuffer) {
        byte[] apdu = new byte[1 + 4 + 1 + 1 + 1 + 1 + 1 + compactFrameBuffer.length];
        int i = 0;
        apdu[i++] = 0x0F;                           // DATA-NOTIFICATION tag
        apdu[i++] = 0x40; apdu[i++] = 0x00; apdu[i++] = 0x00; apdu[i++] = 0x01; // invoke-id
        apdu[i++] = 0x00;                           // date-time: lunghezza 0 (assente)
        apdu[i++] = 0x02;                           // structure
        apdu[i++] = 0x01;                           // 1 elemento
        apdu[i++] = 0x09;                           // octet-string
        apdu[i++] = (byte) compactFrameBuffer.length;
        System.arraycopy(compactFrameBuffer, 0, apdu, i, compactFrameBuffer.length);
        return apdu;
    }

    /** Costruisce un compact frame CF8 minimo (4 byte). */
    private byte[] buildCF8() {
        return new byte[]{ 8, 1, 3, 7 }; // template=8, output=1, control=3, closure=7
    }

    /** Costruisce un compact frame CF47 minimo (29 byte). */
    private byte[] buildCF47Minimal() {
        // template_id(1) + unix_time(4) + pp4_status(2) + valve_output(1) + valve_control(1)
        // + metro_evt(2) + evt_cnt(2) + daily_diag(2) + conv_vol(4) + conv_vol_alarm(4)
        // + billing_cnt(1) + mgmt_fc(4) = 28 byte + 1 spare = 29 byte
        ByteBuffer buf = ByteBuffer.allocate(29).order(ByteOrder.BIG_ENDIAN);
        buf.put((byte) 47);              // template_id
        buf.putInt(1700000000);          // unix_time
        buf.putShort((short) 1);         // pp4_status (uint16)
        buf.put((byte) 1);              // valve_output (boolean)
        buf.put((byte) 2);              // valve_control (uint8)
        buf.putShort((short) 3);         // metro_evt_cnt (uint16)
        buf.putShort((short) 4);         // evt_cnt (uint16)
        buf.putShort((short) 0x1234);   // daily_diag (uint16)
        buf.putInt(500000);             // conv_vol (uint32)
        buf.putInt(1000);               // conv_vol_alarm (uint32)
        buf.put((byte) 5);              // billing_cnt (uint8)
        buf.putInt(42);                 // mgmt_fc (uint32)
        return buf.array();
    }

    // -------------------------------------------------------
    // Costruzione UdpServer per test (porta 0 = server disabilitato)
    // -------------------------------------------------------

    private UdpServer buildServer(TelemetryService ts) {
        UdpServerConfig config = new UdpServerConfig();
        config.setPort(0);
        return new UdpServer(config, ts);
    }

    // -------------------------------------------------------
    // Test esistenti
    // -------------------------------------------------------

    @Test
    void serverDoesNotStartWhenPortIsZero() throws Exception {
        UdpServer server = buildServer(null);
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

    // -------------------------------------------------------
    // Test per handlePacket
    // -------------------------------------------------------

    @Test
    void handlePacket_tooShort_doesNotSave() throws Exception {
        TelemetryService ts = mock(TelemetryService.class);
        UdpServer server = buildServer(ts);

        InetAddress addr = InetAddress.getByName("10.0.0.1");
        // Invoca handlePacket tramite riflessione (metodo privato)
        invokeHandlePacket(server, new byte[3], addr, 60104);

        verifyNoInteractions(ts);
    }

    @Test
    void handlePacket_invalidWrapperVersion_doesNotSave() throws Exception {
        TelemetryService ts = mock(TelemetryService.class);
        UdpServer server = buildServer(ts);

        // WRAPPER con version = 0x0002 (non valida)
        byte[] data = new byte[]{0x00, 0x02, 0x00, 0x01, 0x00, 0x01, 0x00, 0x00};
        InetAddress addr = InetAddress.getByName("10.0.0.1");
        invokeHandlePacket(server, data, addr, 60104);

        verifyNoInteractions(ts);
    }

    @Test
    void handlePacket_unknownApduTag_doesNotSave() throws Exception {
        TelemetryService ts = mock(TelemetryService.class);
        UdpServer server = buildServer(ts);

        byte[] payload = new byte[]{0x01}; // tag non DATA-NOTIFICATION
        InetAddress addr = InetAddress.getByName("10.0.0.1");
        invokeHandlePacket(server, wrapPacket(payload), addr, 60104);

        verifyNoInteractions(ts);
    }

    @Test
    void handlePacket_dataNotificationWithCF8_savesTelemetry() throws Exception {
        TelemetryService ts = mock(TelemetryService.class);
        when(ts.save(any(), any(), any(), any(), anyInt(), any(), anyDouble(), any(), any()))
                .thenReturn(new TelemetryData());
        UdpServer server = buildServer(ts);

        byte[] cf8 = buildCF8();
        byte[] apdu = buildDataNotification(cf8);
        InetAddress addr = InetAddress.getByName("10.0.0.5");
        invokeHandlePacket(server, wrapPacket(apdu), addr, 60104);

        // CF8 ha 3 campi scalari (output, control, closure)
        verify(ts, times(3)).save(eq("10.0.0.5"), eq("10.0.0.5"), any(), any(), eq(62),
                any(), anyDouble(), isNull(), any(Instant.class));
    }

    @Test
    void handlePacket_dataNotificationWithCF47_savesAllFields() throws Exception {
        TelemetryService ts = mock(TelemetryService.class);
        when(ts.save(any(), any(), any(), any(), anyInt(), any(), anyDouble(), any(), any()))
                .thenReturn(new TelemetryData());
        UdpServer server = buildServer(ts);

        byte[] cf47 = buildCF47Minimal();
        byte[] apdu = buildDataNotification(cf47);
        InetAddress addr = InetAddress.getByName("10.0.0.2");
        invokeHandlePacket(server, wrapPacket(apdu), addr, 60104);

        // CF47 ha 11 campi scalari
        verify(ts, times(11)).save(eq("10.0.0.2"), eq("10.0.0.2"), any(), any(), eq(62),
                any(), anyDouble(), isNull(), any(Instant.class));
    }

    @Test
    void handlePacket_dataNotificationWithUnknownTemplate_doesNotSave() throws Exception {
        TelemetryService ts = mock(TelemetryService.class);
        UdpServer server = buildServer(ts);

        // Template ID 99 non e' supportato
        byte[] unknownCf = new byte[]{99, 0x01, 0x02};
        byte[] apdu = buildDataNotification(unknownCf);
        InetAddress addr = InetAddress.getByName("10.0.0.3");
        invokeHandlePacket(server, wrapPacket(apdu), addr, 60104);

        verifyNoInteractions(ts);
    }

    @Test
    void handlePacket_dataNotificationNestedInStructure_findsCF() throws Exception {
        TelemetryService ts = mock(TelemetryService.class);
        when(ts.save(any(), any(), any(), any(), anyInt(), any(), anyDouble(), any(), any()))
                .thenReturn(new TelemetryData());
        UdpServer server = buildServer(ts);

        byte[] cf8 = buildCF8();

        // Costruisce un APDU DATA-NOTIFICATION con struttura annidata:
        // structure(2) { structure(1) { null-data }, octet-string(cf8) }
        int apduSize = 1 + 4 + 1 + 1 + 1 + 1 + 2 + 1 + (1 + cf8.length);
        ByteBuffer buf = ByteBuffer.allocate(apduSize);
        buf.put((byte) 0x0F);                   // DATA-NOTIFICATION
        buf.put((byte) 0x40).put((byte) 0).put((byte) 0).put((byte) 1); // invoke-id
        buf.put((byte) 0x00);                   // no date-time
        buf.put((byte) 0x02).put((byte) 2);     // structure(2)
        buf.put((byte) 0x02).put((byte) 1).put((byte) 0x00); // structure(1) { null-data }
        buf.put((byte) 0x09).put((byte) cf8.length); // octet-string
        buf.put(cf8);
        byte[] apdu = buf.array();

        InetAddress addr = InetAddress.getByName("10.0.0.6");
        invokeHandlePacket(server, wrapPacket(apdu), addr, 60104);

        verify(ts, times(3)).save(any(), any(), any(), any(), eq(62),
                any(), anyDouble(), isNull(), any(Instant.class));
    }

    // -------------------------------------------------------
    // Utility per invocare il metodo privato handlePacket
    // -------------------------------------------------------

    private void invokeHandlePacket(UdpServer server, byte[] data, InetAddress addr, int port)
            throws Exception {
        var method = UdpServer.class.getDeclaredMethod("handlePacket", byte[].class,
                InetAddress.class, int.class);
        method.setAccessible(true);
        method.invoke(server, data, addr, port);
    }
}
