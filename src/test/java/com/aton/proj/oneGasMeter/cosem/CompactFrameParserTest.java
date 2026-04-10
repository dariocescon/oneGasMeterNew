package com.aton.proj.oneGasMeter.cosem;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test unitari per CompactFrameParser.
 */
class CompactFrameParserTest {

    @Test
    void parseCF47Basic() {
        byte[] buf = buildCF47(1700000000L, 0x0001, true, 2, 10, 20, 0x1234,
                               500000L, 100L, 5, 42L);

        CompactFrameData data = CompactFrameParser.parse(buf);

        assertNotNull(data);
        assertEquals(47, data.getTemplateId());
        assertEquals(Instant.ofEpochSecond(1700000000L), data.getTimestamp());
        assertEquals(1700000000L, data.getLong("0.0.1.1.0.255"));       // UNIX time
        assertEquals(1, data.getInt("0.1.96.5.4.255"));                 // PP4 Network Status
        assertEquals(true, data.getBoolean("0.0.96.3.10.255#output"));  // Valve output
        assertEquals(2, data.getInt("0.0.96.3.10.255#control"));        // Valve control
        assertEquals(10, data.getInt("0.0.96.15.1.255"));               // Metro Event Cnt
        assertEquals(20, data.getInt("0.0.96.15.2.255"));               // Event Counter
        assertEquals(0x1234, data.getInt("7.1.96.5.1.255"));            // Daily Diagnostic
        assertEquals(500000L, data.getLong("7.0.13.2.0.255"));          // Conv Volume
        assertEquals(100L, data.getLong("7.0.12.2.0.255"));             // Conv Vol Alarm
        assertEquals(5, data.getInt("7.0.0.1.0.255"));                  // Billing Counter
        assertEquals(42L, data.getLong("0.0.43.1.1.255"));              // Mgmt FC Online
    }

    @Test
    void parseCF49HasMoreFieldsThanCF47() {
        // CF49 = CF47 fields + daily profile (43 byte) + billing counter + snapshot (51 byte) + FC
        byte[] cf47 = buildCF47(1700000000L, 1, false, 0, 0, 0, 0, 0L, 0L, 0, 0L);
        byte[] cf49Buf = buildCF49Minimal();

        CompactFrameData data47 = CompactFrameParser.parse(cf47);
        CompactFrameData data49 = CompactFrameParser.parse(cf49Buf);

        assertNotNull(data47);
        assertNotNull(data49);
        assertEquals(47, data47.getTemplateId());
        assertEquals(49, data49.getTemplateId());
        // CF49 ha piu' campi di CF47
        assertTrue(data49.getValues().size() >= data47.getValues().size());
    }

    @Test
    void parseCF8ValveStatus() {
        byte[] buf = new byte[]{
            8,      // template_id
            1,      // output_state = true
            3,      // control_state = 3
            7       // closure cause = 7
        };

        CompactFrameData data = CompactFrameParser.parse(buf);

        assertNotNull(data);
        assertEquals(8, data.getTemplateId());
        assertEquals(true, data.getBoolean("0.0.96.3.10.255#output"));
        assertEquals(3, data.getInt("0.0.96.3.10.255#control"));
        assertEquals(7, data.getInt("0.0.94.39.7.255"));
    }

    @Test
    void parseNullReturnsNull() {
        assertNull(CompactFrameParser.parse(null));
    }

    @Test
    void parseTooShortReturnsNull() {
        assertNull(CompactFrameParser.parse(new byte[]{1}));
    }

    @Test
    void parseUnknownTemplateReturnsNull() {
        assertNull(CompactFrameParser.parse(new byte[]{(byte) 99, 0}));
    }

    @Test
    void cf47TimestampConversion() {
        long epoch = 1710000000L; // 2024-03-09
        byte[] buf = buildCF47(epoch, 0, false, 0, 0, 0, 0, 0L, 0L, 0, 0L);

        CompactFrameData data = CompactFrameParser.parse(buf);

        assertNotNull(data);
        assertEquals(Instant.ofEpochSecond(epoch), data.getTimestamp());
    }

    // === Utility per costruire buffer di test ===

    private byte[] buildCF47(long unixTime, int networkStatus, boolean valveOutput,
                              int valveControl, int metroEvt, int evtCnt, int dailyDiag,
                              long convVol, long convVolAlarm, int billingCnt, long mgmtFc) {
        ByteBuffer bb = ByteBuffer.allocate(29).order(ByteOrder.BIG_ENDIAN);
        bb.put((byte) 47);                      // template_id
        bb.putInt((int) unixTime);               // UNIX time
        bb.putShort((short) networkStatus);      // PP4 Network Status
        bb.put((byte) (valveOutput ? 1 : 0));    // valve output_state
        bb.put((byte) valveControl);             // valve control_state
        bb.putShort((short) metroEvt);           // metrological event counter
        bb.putShort((short) evtCnt);             // event counter
        bb.putShort((short) dailyDiag);          // daily diagnostic
        bb.putInt((int) convVol);                // current index converted volume
        bb.putInt((int) convVolAlarm);           // current index conv vol alarm
        bb.put((byte) billingCnt);               // billing period counter
        bb.putInt((int) mgmtFc);                 // mgmt frame counter online
        return bb.array();
    }

    private byte[] buildCF49Minimal() {
        // CF49: template(1) + base fields(27) + daily profile(43) + billing(1) + snapshot(51) + FC(4)
        ByteBuffer bb = ByteBuffer.allocate(1 + 27 + 43 + 1 + 51 + 4).order(ByteOrder.BIG_ENDIAN);
        bb.put((byte) 49);                       // template_id
        bb.putInt(1700000000);                    // UNIX time
        bb.putShort((short) 0);                   // PP4 Network Status
        bb.put((byte) 0);                         // valve output
        bb.put((byte) 0);                         // valve control
        bb.putShort((short) 0);                   // metro event cnt
        bb.putShort((short) 0);                   // event cnt
        bb.putShort((short) 0);                   // daily diagnostic
        bb.putInt(0);                             // conv vol
        bb.putInt(0);                             // conv vol alarm
        bb.put(new byte[43]);                     // daily profile (zeros)
        bb.put((byte) 0);                         // billing counter
        bb.put(new byte[51]);                     // snapshot data (zeros)
        bb.putInt(100);                           // mgmt FC online
        return bb.array();
    }
}
