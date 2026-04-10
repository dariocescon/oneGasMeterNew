package com.aton.proj.oneGasMeter.cosem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Instant;

/**
 * Parser per le Compact Frame (Class 62) secondo UNI/TS 11291-12-2:2020.
 *
 * Le compact frame contengono dati senza type tag: il SAC conosce il template
 * e sa come interpretare ogni byte. Il template_id (primo byte) identifica
 * quale struttura usare.
 *
 * Formati dati DLMS usati nelle compact frame:
 * - unsigned:              1 byte, senza segno (0..255)
 * - long-unsigned:         2 byte big-endian, senza segno (0..65535)
 * - double-long-unsigned:  4 byte big-endian, senza segno (0..4294967295)
 * - boolean:               1 byte (0=false, altrimenti true)
 * - enum:                  1 byte
 * - octet-string:          prefisso lunghezza (1 byte) + dati
 */
public class CompactFrameParser {

    private static final Logger log = LoggerFactory.getLogger(CompactFrameParser.class);

    private CompactFrameParser() {
        // Utility class
    }

    /**
     * Parsa una compact frame dal compact_buffer (senza il tipo/tag DLMS).
     * Il primo byte e' il template_id che identifica la struttura.
     *
     * @param buffer compact_buffer ricevuto dal contatore
     * @return dati parsati, oppure null se il template non e' supportato
     */
    public static CompactFrameData parse(byte[] buffer) {
        if (buffer == null || buffer.length < 2) {
            log.warn("Compact frame troppo corta: {} byte", buffer == null ? 0 : buffer.length);
            return null;
        }

        int templateId = Byte.toUnsignedInt(buffer[0]);

        return switch (templateId) {
            case 47 -> parseCF47(buffer);
            case 48 -> parseCF48(buffer);
            case 49 -> parseCF49(buffer);
            case 8  -> parseCF8(buffer);
            case 51 -> parseCF51(buffer);
            default -> {
                log.warn("Template compact frame non supportato: {}", templateId);
                yield null;
            }
        };
    }

    /**
     * CF47 - Content A PP4 (push base).
     *
     * Layout (28+ byte):
     * [0]    template_id       unsigned         (1 byte) = 47
     * [1-4]  UNIX time         double-long-uns  (4 byte)
     * [5-6]  PP4 Network Status long-unsigned   (2 byte)
     * [7]    valve output_state boolean          (1 byte)
     * [8]    valve control_state enum            (1 byte)
     * [9-10] metrological evt cnt long-unsigned  (2 byte)
     * [11-12] event counter    long-unsigned     (2 byte)
     * [13-14] daily diagnostic long-unsigned     (2 byte)
     * [15-18] curr idx conv vol double-long-uns  (4 byte)
     * [19-22] curr idx conv vol alarm dlu        (4 byte)
     * [23]   billing period cnt unsigned         (1 byte)
     * [24-27] mgmt FC online   double-long-uns  (4 byte)
     * [28]   spare             octet-string      (1+ byte)
     */
    static CompactFrameData parseCF47(byte[] buf) {
        CompactFrameData data = new CompactFrameData(47);
        ByteBuffer bb = wrap(buf);
        bb.get(); // skip template_id

        long unixTime = readUint32(bb);
        data.setTimestamp(Instant.ofEpochSecond(unixTime));
        data.put("0.0.1.1.0.255", unixTime);

        data.put("0.1.96.5.4.255", readUint16(bb));          // PP4 Network Status
        data.put("0.0.96.3.10.255#output", readBoolean(bb)); // Valve output_state
        data.put("0.0.96.3.10.255#control", readUint8(bb));  // Valve control_state
        data.put("0.0.96.15.1.255", readUint16(bb));         // Metrological Event Counter
        data.put("0.0.96.15.2.255", readUint16(bb));         // Event Counter
        data.put("7.1.96.5.1.255", readUint16(bb));          // Daily Diagnostic
        data.put("7.0.13.2.0.255", readUint32(bb));          // Current Index Converted Volume
        data.put("7.0.12.2.0.255", readUint32(bb));          // Current Index Conv Vol Alarm
        data.put("7.0.0.1.0.255", readUint8(bb));            // Billing Period Counter

        if (bb.remaining() >= 4) {
            data.put("0.0.43.1.1.255", readUint32(bb));      // Mgmt Frame Counter Online
        }
        // Spare object: ignora il resto

        log.debug("CF47 parsata: {} campi, timestamp={}", data.getValues().size(), data.getTimestamp());
        return data;
    }

    /**
     * CF48 - Content B PP4 (push + ultimi 3 daily profile).
     *
     * Stessa struttura di CF47 fino al frame counter,
     * poi aggiunge il Daily Load Profile (ultimi 3 giorni, 43 byte).
     */
    static CompactFrameData parseCF48(byte[] buf) {
        CompactFrameData data = new CompactFrameData(48);
        ByteBuffer bb = wrap(buf);
        bb.get(); // skip template_id

        // Stessi campi di CF47
        long unixTime = readUint32(bb);
        data.setTimestamp(Instant.ofEpochSecond(unixTime));
        data.put("0.0.1.1.0.255", unixTime);

        data.put("0.1.96.5.4.255", readUint16(bb));
        data.put("0.0.96.3.10.255#output", readBoolean(bb));
        data.put("0.0.96.3.10.255#control", readUint8(bb));
        data.put("0.0.96.15.1.255", readUint16(bb));
        data.put("0.0.96.15.2.255", readUint16(bb));
        data.put("7.1.96.5.1.255", readUint16(bb));
        data.put("7.0.13.2.0.255", readUint32(bb));
        data.put("7.0.12.2.0.255", readUint32(bb));

        // Daily Load Profile (ultimi 3 entries) - salva come byte[] grezzo
        if (bb.remaining() >= 43) {
            byte[] dailyProfile = new byte[43];
            bb.get(dailyProfile);
            data.put("7.0.99.99.3.255", dailyProfile);
        }

        data.put("7.0.0.1.0.255", readUint8Safe(bb));

        if (bb.remaining() >= 4) {
            data.put("0.0.43.1.1.255", readUint32(bb));
        }

        log.debug("CF48 parsata: {} campi, timestamp={}", data.getValues().size(), data.getTimestamp());
        return data;
    }

    /**
     * CF49 - Content C PP4 (push completo + daily + billing + tariffe).
     *
     * Stessa struttura di CF48, poi aggiunge Snapshot Period Data (51 byte).
     */
    static CompactFrameData parseCF49(byte[] buf) {
        CompactFrameData data = new CompactFrameData(49);
        ByteBuffer bb = wrap(buf);
        bb.get(); // skip template_id

        // Stessi campi di CF47/48
        long unixTime = readUint32(bb);
        data.setTimestamp(Instant.ofEpochSecond(unixTime));
        data.put("0.0.1.1.0.255", unixTime);

        data.put("0.1.96.5.4.255", readUint16(bb));
        data.put("0.0.96.3.10.255#output", readBoolean(bb));
        data.put("0.0.96.3.10.255#control", readUint8(bb));
        data.put("0.0.96.15.1.255", readUint16(bb));
        data.put("0.0.96.15.2.255", readUint16(bb));
        data.put("7.1.96.5.1.255", readUint16(bb));
        data.put("7.0.13.2.0.255", readUint32(bb));
        data.put("7.0.12.2.0.255", readUint32(bb));

        // Daily Load Profile (ultimi 3 entries)
        if (bb.remaining() >= 43) {
            byte[] dailyProfile = new byte[43];
            bb.get(dailyProfile);
            data.put("7.0.99.99.3.255", dailyProfile);
        }

        data.put("7.0.0.1.0.255", readUint8Safe(bb));

        // Snapshot Period Data (ultimo entry)
        if (bb.remaining() >= 51) {
            byte[] snapshotData = new byte[51];
            bb.get(snapshotData);
            data.put("7.0.98.11.0.255", snapshotData);
        }

        if (bb.remaining() >= 4) {
            data.put("0.0.43.1.1.255", readUint32(bb));
        }

        log.debug("CF49 parsata: {} campi, timestamp={}", data.getValues().size(), data.getTimestamp());
        return data;
    }

    /**
     * CF8 - Valve Status.
     *
     * Layout:
     * [0]   template_id       unsigned   (1 byte) = 8
     * [1]   valve output_state boolean   (1 byte)
     * [2]   valve control_state enum     (1 byte)
     * [3]   valve closure cause unsigned (1 byte)
     * [4]   spare             octet-str  (1+ byte)
     */
    static CompactFrameData parseCF8(byte[] buf) {
        CompactFrameData data = new CompactFrameData(8);
        ByteBuffer bb = wrap(buf);
        bb.get(); // skip template_id

        data.put("0.0.96.3.10.255#output", readBoolean(bb));
        data.put("0.0.96.3.10.255#control", readUint8(bb));
        data.put("0.0.94.39.7.255", readUint8(bb));

        log.debug("CF8 parsata: {} campi", data.getValues().size());
        return data;
    }

    /**
     * CF51 - Frame Counter Values.
     *
     * Layout:
     * [0]      template_id             unsigned         (1 byte) = 51
     * [1-17]   COSEM logical dev name  octet-string     (2+15 byte)
     * [18-21]  Mgmt FC Online          double-long-uns  (4 byte)
     * [22-25]  Mgmt FC Offline         double-long-uns  (4 byte)
     * [26-29]  GA FC                   double-long-uns  (4 byte)
     * [30-33]  I/M FC                  double-long-uns  (4 byte)
     */
    static CompactFrameData parseCF51(byte[] buf) {
        CompactFrameData data = new CompactFrameData(51);
        ByteBuffer bb = wrap(buf);
        bb.get(); // skip template_id

        // Logical Device Name: length-prefixed octet-string
        if (bb.remaining() >= 2) {
            int len = readUint16(bb);
            if (bb.remaining() >= len) {
                byte[] ldnBytes = new byte[len];
                bb.get(ldnBytes);
                data.put("0.0.42.0.0.255", new String(ldnBytes));
            }
        }

        if (bb.remaining() >= 4) data.put("0.0.43.1.1.255", readUint32(bb));
        if (bb.remaining() >= 4) data.put("0.1.43.1.1.255", readUint32(bb));
        if (bb.remaining() >= 4) data.put("0.0.43.1.48.255", readUint32(bb));
        if (bb.remaining() >= 4) data.put("0.0.43.1.3.255", readUint32(bb));

        log.debug("CF51 parsata: {} campi", data.getValues().size());
        return data;
    }

    // === Metodi di lettura dal ByteBuffer ===

    private static ByteBuffer wrap(byte[] buf) {
        return ByteBuffer.wrap(buf).order(ByteOrder.BIG_ENDIAN);
    }

    /** Legge 1 byte senza segno (0..255). */
    private static int readUint8(ByteBuffer bb) {
        return Byte.toUnsignedInt(bb.get());
    }

    /** Legge 1 byte senza segno, restituisce 0 se non disponibile. */
    private static int readUint8Safe(ByteBuffer bb) {
        return bb.hasRemaining() ? Byte.toUnsignedInt(bb.get()) : 0;
    }

    /** Legge 2 byte big-endian senza segno (0..65535). */
    private static int readUint16(ByteBuffer bb) {
        return Short.toUnsignedInt(bb.getShort());
    }

    /** Legge 4 byte big-endian senza segno (0..4294967295). */
    private static long readUint32(ByteBuffer bb) {
        return Integer.toUnsignedLong(bb.getInt());
    }

    /** Legge 1 byte come boolean (0=false, altrimenti true). */
    private static boolean readBoolean(ByteBuffer bb) {
        return bb.get() != 0;
    }
}
