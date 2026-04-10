package com.aton.proj.oneGasMeter.cosem;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test unitari per CompactFrameParser.
 * Tutti i codici OBIS usati per le asserzioni provengono dall'enum CosemObject.
 */
class CompactFrameParserTest {

    // Alias per le costanti OBIS usate nelle asserzioni
    private static final String VALVE_OBIS = CosemObject.VALVE_STATE.getObisCode();

    @Test
    void parseCF47Basic() {
        byte[] buf = buildCF47(1700000000L, 0x0001, true, 2, 10, 20, 0x1234,
                               500000L, 100L, 5, 42L);

        CompactFrameData data = CompactFrameParser.parse(buf);

        assertNotNull(data);
        assertEquals(47, data.getTemplateId());
        assertEquals(Instant.ofEpochSecond(1700000000L), data.getTimestamp());
        assertEquals(1700000000L, data.getLong(CosemObject.UNIX_TIME.getObisCode()));                  // UNIX time         
        assertEquals(1, data.getInt(CosemObject.PP4_NETWORK_STATUS.getObisCode()));                    // PP4 Network Status
        assertEquals(true, data.getBoolean(VALVE_OBIS + "#output"));                                   // Valve output      
        assertEquals(2, data.getInt(VALVE_OBIS + "#control"));                                         // Valve control     
        assertEquals(10, data.getInt(CosemObject.METROLOGICAL_EVENT_COUNTER.getObisCode()));           // Metro Event Cnt   
        assertEquals(20, data.getInt(CosemObject.EVENT_COUNTER.getObisCode()));                        // Event Counter     
        assertEquals(0x1234, data.getInt(CosemObject.DAILY_DIAGNOSTIC.getObisCode()));                 // Daily Diagnostic  
        assertEquals(500000L, data.getLong(CosemObject.CURRENT_INDEX_CONVERTED_VOL.getObisCode()));    // Conv Volume       
        assertEquals(100L, data.getLong(CosemObject.CURRENT_INDEX_CONV_VOL_ALARM.getObisCode()));      // Conv Vol Alarm    
        assertEquals(5, data.getInt(CosemObject.BILLING_PERIOD_COUNTER.getObisCode()));                // Billing Counter   
        assertEquals(42L, data.getLong(CosemObject.MGMT_FRAME_COUNTER_ONLINE.getObisCode()));          // Mgmt FC Online    
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
        assertEquals(true, data.getBoolean(VALVE_OBIS + "#output"));
        assertEquals(3, data.getInt(VALVE_OBIS + "#control"));
        assertEquals(7, data.getInt(CosemObject.VALVE_CLOSURE_CAUSE.getObisCode()));
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

    @Test
    void parseCF7ValveProgramming() {
        ByteBuffer bb = ByteBuffer.allocate(12).order(ByteOrder.BIG_ENDIAN);
        bb.put((byte) 7);           // template_id
        bb.putShort((short) 1234);  // Valve Config PGV
        bb.put((byte) 5);           // Max Password Attempts
        bb.put((byte) 1);           // array count
        bb.putShort((short) 30);    // Days Without Comms = 30
        bb.put((byte) 1);           // array count
        bb.putInt(100);             // Tampering Attempts = 100

        CompactFrameData data = CompactFrameParser.parse(bb.array());

        assertNotNull(data);
        assertEquals(7, data.getTemplateId());
        assertEquals(1234, data.getInt(CosemObject.VALVE_CONFIG_PGV.getObisCode()));               // PGV                
        assertEquals(5, data.getInt(CosemObject.VALVE_MAX_PASSWORD_ATTEMPTS.getObisCode()));       // Max Password       
        assertEquals(30, data.getInt(CosemObject.DAYS_WITHOUT_COMMS_THRESHOLD.getObisCode()));     // Days Without Comms 
        assertEquals(100L, data.getLong(CosemObject.TAMPERING_ATTEMPTS_THRESHOLD.getObisCode()));  // Tampering Threshold
    }

    @Test
    void parseCF9ValveManagement() {
        ByteBuffer bb = ByteBuffer.allocate(30).order(ByteOrder.BIG_ENDIAN);
        bb.put((byte) 9);             // template_id
        bb.put(new byte[13]);         // script reference (zeros)
        bb.put(new byte[12]);         // execution time (zeros)
        bb.putShort((short) 9999);    // Valve Enable Password
        bb.putShort((short) 120);     // Opening Command Duration

        CompactFrameData data = CompactFrameParser.parse(bb.array());

        assertNotNull(data);
        assertEquals(9, data.getTemplateId());
        assertEquals(9999, data.getInt(CosemObject.VALVE_ENABLE_PASSWORD.getObisCode()));
        assertEquals(120, data.getInt(CosemObject.VALVE_OPENING_DURATION.getObisCode()));
    }

    @Test
    void parseCF4EobParameters() {
        ByteBuffer bb = ByteBuffer.allocate(24).order(ByteOrder.BIG_ENDIAN);
        bb.put((byte) 4);            // template_id
        bb.putShort((short) 30);     // EOB Snapshot Period = 30 giorni
        bb.put(new byte[5]);         // EOB Starting Date
        bb.put(new byte[4]);         // Start Gas Day
        bb.put(new byte[12]);        // On Demand Snapshot Time

        CompactFrameData data = CompactFrameParser.parse(bb.array());

        assertNotNull(data);
        assertEquals(4, data.getTemplateId());
        assertEquals(30, data.getInt(CosemObject.EOB_SNAPSHOT_PERIOD.getObisCode()));
    }

    @Test
    void parseCF5ActiveTariff() {
        byte[] buf = new byte[20];
        buf[0] = 5; // template_id
        // resto e' il piano tariffario (raw)

        CompactFrameData data = CompactFrameParser.parse(buf);

        assertNotNull(data);
        assertEquals(5, data.getTemplateId());
        assertNotNull(data.get(CosemObject.ACTIVE_TARIFF_PLAN.getObisCode()));
    }

    @Test
    void parseCF6PassiveTariff() {
        byte[] buf = new byte[20];
        buf[0] = 6;

        CompactFrameData data = CompactFrameParser.parse(buf);

        assertNotNull(data);
        assertEquals(6, data.getTemplateId());
        assertNotNull(data.get(CosemObject.PASSIVE_TARIFF_PLAN.getObisCode()));
    }

    @Test
    void parseCF41CommSetup() {
        // CF41: template(1) + 4 * (scheduler 45 + setup 23) = 273 byte
        ByteBuffer bb = ByteBuffer.allocate(273).order(ByteOrder.BIG_ENDIAN);
        bb.put((byte) 41);
        for (int i = 0; i < 4; i++) {
            bb.put(new byte[45]); // scheduler
            bb.put(new byte[23]); // setup
        }

        CompactFrameData data = CompactFrameParser.parse(bb.array());

        assertNotNull(data);
        assertEquals(41, data.getTemplateId());
        // 4 scheduler + 4 setup = 8 entries
        assertEquals(8, data.getValues().size());
    }

    @Test
    void parseCF22FwTransferStatus() {
        ByteBuffer bb = ByteBuffer.allocate(6).order(ByteOrder.BIG_ENDIAN);
        bb.put((byte) 22);          // template_id
        bb.put((byte) 3);           // image_transfer_status = verification_successful
        bb.putShort((short) 2);     // bit-string length = 2
        bb.put((byte) 0xFF);        // blocks status byte 1
        bb.put((byte) 0xF0);        // blocks status byte 2

        CompactFrameData data = CompactFrameParser.parse(bb.array());

        assertNotNull(data);
        assertEquals(22, data.getTemplateId());
        String fwObis = CosemObject.IMAGE_TRANSFER.getObisCode(); // verification_successful
        assertEquals(3, data.getInt(fwObis + "#status"));
        assertNotNull(data.get(fwObis + "#blocks"));
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
