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
 *
 * Tutti i codici OBIS usati come chiave nei dati parsati provengono dall'enum CosemObject.
 */
public class CompactFrameParser {

    private static final Logger log = LoggerFactory.getLogger(CompactFrameParser.class);

    // Alias brevi per le costanti OBIS usate frequentemente nei parser
    private static final String OBIS_UNIX_TIME = CosemObject.UNIX_TIME.getObisCode();
    private static final String OBIS_PP4_STATUS = CosemObject.PP4_NETWORK_STATUS.getObisCode();
    private static final String OBIS_VALVE = CosemObject.VALVE_STATE.getObisCode();
    private static final String OBIS_METRO_EVT_CNT = CosemObject.METROLOGICAL_EVENT_COUNTER.getObisCode();
    private static final String OBIS_EVT_CNT = CosemObject.EVENT_COUNTER.getObisCode();
    private static final String OBIS_DAILY_DIAG = CosemObject.DAILY_DIAGNOSTIC.getObisCode();
    private static final String OBIS_CONV_VOL = CosemObject.CURRENT_INDEX_CONVERTED_VOL.getObisCode();
    private static final String OBIS_CONV_VOL_ALARM = CosemObject.CURRENT_INDEX_CONV_VOL_ALARM.getObisCode();
    private static final String OBIS_BILLING_CNT = CosemObject.BILLING_PERIOD_COUNTER.getObisCode();
    private static final String OBIS_MGMT_FC = CosemObject.MGMT_FRAME_COUNTER_ONLINE.getObisCode();
    private static final String OBIS_DAILY_PROFILE = CosemObject.DAILY_LOAD_PROFILE.getObisCode();
    private static final String OBIS_SNAPSHOT = CosemObject.SNAPSHOT_PERIOD_DATA.getObisCode();

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
            case 3  -> parseCF3(buffer);
            case 4  -> parseCF4(buffer);
            case 5  -> parseCF5(buffer);
            case 6  -> parseCF6(buffer);
            case 7  -> parseCF7(buffer);
            case 8  -> parseCF8(buffer);
            case 9  -> parseCF9(buffer);
            case 22 -> parseCF22(buffer);
            case 41 -> parseCF41(buffer);
            case 47 -> parseCF47(buffer);
            case 48 -> parseCF48(buffer);
            case 49 -> parseCF49(buffer);
            case 51 -> parseCF51(buffer);
            default -> {
                log.warn("Template compact frame non supportato: {}", templateId);
                yield null;
            }
        };
    }

    /**
     * CF3 - Diagnostics and Alarms.
     *
     * Contiene: batterie (use time, remaining), tempo operativo totale,
     * contatore tamper comunicazione, dati monitoraggio comunicazione e SLA.
     */
    static CompactFrameData parseCF3(byte[] buf) {
        CompactFrameData data = new CompactFrameData(3);
        ByteBuffer bb = wrap(buf);
        bb.get(); // skip template_id

        if (bb.remaining() >= 4) data.put(CosemObject.BATTERY_USE_TIME_0.getObisCode(), readUint32(bb));
        if (bb.remaining() >= 4) data.put(CosemObject.BATTERY_USE_TIME_1.getObisCode(), readUint32(bb));
        if (bb.remaining() >= 2) data.put(CosemObject.BATTERY_REMAINING_0.getObisCode(), readUint16(bb));
        if (bb.remaining() >= 2) data.put(CosemObject.BATTERY_REMAINING_1.getObisCode(), readUint16(bb));
        if (bb.remaining() >= 4) data.put(CosemObject.TOTAL_OPERATING_TIME.getObisCode(), readUint32(bb));
        if (bb.remaining() >= 2) data.put(CosemObject.COMM_TAMPER_EVENT_COUNTER.getObisCode(), readUint16(bb));

        // Monitoring Communication Data e SLA Data: strutture variabili, salva come byte[]
        if (bb.remaining() > 0) {
            byte[] rest = new byte[bb.remaining()];
            bb.get(rest);
            data.put(CosemObject.MONITORING_COMM_DATA.getObisCode(), rest); // raw monitoring data
        }

        log.debug("CF3 parsata: {} campi", data.getValues().size());
        return data;
    }

    /**
     * CF4 - EOB Parameters.
     *
     * Contiene: periodo snapshot EOB, data inizio, data/ora snapshot su richiesta,
     * ora inizio giorno gas, parametri orologio.
     * La struttura interna e' complessa (date, time). Salviamo i campi principali.
     */
    static CompactFrameData parseCF4(byte[] buf) {
        CompactFrameData data = new CompactFrameData(4);
        ByteBuffer bb = wrap(buf);
        bb.get(); // skip template_id

        // EOB Snapshot Period (long-unsigned, giorni)
        if (bb.remaining() >= 2) data.put(CosemObject.EOB_SNAPSHOT_PERIOD.getObisCode(), readUint16(bb));

        // EOB Snapshot Starting Date (date = 5 byte)
        if (bb.remaining() >= 5) {
            byte[] startDate = new byte[5];
            bb.get(startDate);
            data.put(CosemObject.EOB_SNAPSHOT_STARTING_DATE.getObisCode(), startDate);
        }

        // Start of Conventional Gas Day (time = 4 byte)
        if (bb.remaining() >= 4) {
            byte[] gasDay = new byte[4];
            bb.get(gasDay);
            data.put(CosemObject.START_GAS_DAY.getObisCode(), gasDay);
        }

        // On Demand Snapshot Time (date-time = 12 byte)
        if (bb.remaining() >= 12) {
            byte[] snapshotTime = new byte[12];
            bb.get(snapshotTime);
            data.put(CosemObject.ON_DEMAND_SNAPSHOT_TIME.getObisCode(), snapshotTime);
        }

        // Il resto contiene parametri orologio (variabili), salva come raw
        if (bb.remaining() > 0) {
            byte[] rest = new byte[bb.remaining()];
            bb.get(rest);
            data.put("clock_params", rest);
        }

        log.debug("CF4 parsata: {} campi", data.getValues().size());
        return data;
    }

    /**
     * CF5 - Active Tariff Plan (sola lettura).
     *
     * Contiene il piano tariffario attivo (classe proprietaria 8192).
     * La struttura interna e' complessa: calendar_name, enabled, plan, activation_date_time.
     * Salviamo il buffer completo come raw per parsing applicativo esterno.
     */
    static CompactFrameData parseCF5(byte[] buf) {
        CompactFrameData data = new CompactFrameData(5);
        ByteBuffer bb = wrap(buf);
        bb.get(); // skip template_id

        // Tutto il payload e' la struttura del piano tariffario attivo
        if (bb.remaining() > 0) {
            byte[] tariffData = new byte[bb.remaining()];
            bb.get(tariffData);
            data.put(CosemObject.ACTIVE_TARIFF_PLAN.getObisCode(), tariffData);
        }

        log.debug("CF5 parsata: piano tariffario attivo ({} byte)", buf.length - 1);
        return data;
    }

    /**
     * CF6 - Passive Tariff Plan (lettura/scrittura).
     *
     * Stessa struttura di CF5 ma per il piano tariffario passivo (da programmare).
     */
    static CompactFrameData parseCF6(byte[] buf) {
        CompactFrameData data = new CompactFrameData(6);
        ByteBuffer bb = wrap(buf);
        bb.get(); // skip template_id

        if (bb.remaining() > 0) {
            byte[] tariffData = new byte[bb.remaining()];
            bb.get(tariffData);
            data.put(CosemObject.PASSIVE_TARIFF_PLAN.getObisCode(), tariffData);
        }

        log.debug("CF6 parsata: piano tariffario passivo ({} byte)", buf.length - 1);
        return data;
    }

    /**
     * CF22 - FW DLMS Transfer Status.
     *
     * Layout:
     * [0]   template_id                    unsigned   (1 byte) = 22
     * [1]   image_transfer_status          enum       (1 byte)
     * [2..] image_transferred_blocks_status bit-string (length-prefixed)
     * [..]  spare                          octet-str  (1+ byte)
     *
     * image_transfer_status values:
     *   0 = image_transfer_not_initiated
     *   1 = image_transfer_initiated
     *   2 = image_verification_initiated
     *   3 = image_verification_successful
     *   4 = image_verification_failed
     *   5 = image_activation_initiated
     *   6 = image_activation_successful
     *   7 = image_activation_failed
     */
    static CompactFrameData parseCF22(byte[] buf) {
        CompactFrameData data = new CompactFrameData(22);
        ByteBuffer bb = wrap(buf);
        bb.get(); // skip template_id

        // image_transfer_status
        String fwObis = CosemObject.IMAGE_TRANSFER.getObisCode();
        if (bb.remaining() >= 1) {
            data.put(fwObis + "#status", readUint8(bb));
        }

        // image_transferred_blocks_status: length-prefixed bit-string
        if (bb.remaining() >= 2) {
            int len = readUint16(bb);
            if (len > 0 && bb.remaining() >= len) {
                byte[] blocks = new byte[len];
                bb.get(blocks);
                data.put(fwObis + "#blocks", blocks);
            }
        }

        log.debug("CF22 parsata: {} campi", data.getValues().size());
        return data;
    }

    /**
     * CF41 - Communication Setup PP4.
     *
     * Contiene la configurazione di tutti e 4 i push setup/scheduler.
     * Per ogni push (1-4): scheduler execution_time, push_object_list,
     * send_destination_and_method, randomisation_start_interval,
     * number_of_retries, repetition_delay.
     *
     * La struttura e' molto lunga e variabile. Estraiamo i campi chiave
     * per ogni push setup.
     */
    static CompactFrameData parseCF41(byte[] buf) {
        CompactFrameData data = new CompactFrameData(41);
        ByteBuffer bb = wrap(buf);
        bb.get(); // skip template_id

        // 4 push setup, ciascuno con scheduler + setup attributes
        // La struttura esatta dipende dal contenuto variabile delle execution_time
        // e push_object_list. Salviamo come blocco raw per ogni push.
        String[] pushSchedulerObis = {
            CosemObject.PUSH_SCHEDULER_1.getObisCode(),
            CosemObject.PUSH_SCHEDULER_2.getObisCode(),
            CosemObject.PUSH_SCHEDULER_3.getObisCode(),
            CosemObject.PUSH_SCHEDULER_4.getObisCode()
        };
        String[] pushSetupObis = {
            CosemObject.PUSH_SETUP_1.getObisCode(),
            CosemObject.PUSH_SETUP_2.getObisCode(),
            CosemObject.PUSH_SETUP_3.getObisCode(),
            CosemObject.PUSH_SETUP_4.getObisCode()
        };

        // Ogni push block: scheduler(45 byte) + setup(~68 byte) = ~113 byte
        // Ma la dimensione e' variabile. Salviamo tutto come raw per push.
        for (int i = 0; i < 4 && bb.remaining() > 0; i++) {
            // Scheduler execution_time: 45 byte (array of 4 time+date pairs)
            if (bb.remaining() >= 45) {
                byte[] schedData = new byte[45];
                bb.get(schedData);
                data.put(pushSchedulerObis[i], schedData);
            }

            // Push Setup attributes: push_object_list(13) + destination(5) +
            // randomisation(2) + retries(1) + delay(2) = 23 byte minimo
            if (bb.remaining() >= 23) {
                byte[] setupData = new byte[23];
                bb.get(setupData);
                data.put(pushSetupObis[i], setupData);
            }
        }

        // Spare object alla fine
        // ignorato

        log.debug("CF41 parsata: {} campi", data.getValues().size());
        return data;
    }

    /**
     * CF7 - Valve Programming.
     *
     * Layout:
     * [0]    template_id                  unsigned          (1 byte) = 7
     * [1-2]  Valve Configuration PGV      long-unsigned     (2 byte)
     * [3]    Maximum Password Attempts    unsigned          (1 byte)
     * [4-6]  Days Without Comms Threshold array 1 l-u       (3 byte: 1 len + 2 value)
     * [7-11] Tampering Attempts Threshold array 1 dlu       (5 byte: 1 len + 4 value)
     * [12-15] Leakage Test Parameters     structure         (4 byte)
     * [16]   spare                        octet-string      (1+ byte)
     */
    static CompactFrameData parseCF7(byte[] buf) {
        CompactFrameData data = new CompactFrameData(7);
        ByteBuffer bb = wrap(buf);
        bb.get(); // skip template_id

        if (bb.remaining() >= 2) data.put(CosemObject.VALVE_CONFIG_PGV.getObisCode(), readUint16(bb));            // Valve Config PGV
        if (bb.remaining() >= 1) data.put(CosemObject.VALVE_MAX_PASSWORD_ATTEMPTS.getObisCode(), readUint8(bb));  // Max Password Attempts

        // Days Without Comms Threshold: array of 1 long-unsigned (1 byte count + 2 byte value)
        if (bb.remaining() >= 3) {
            int count = readUint8(bb);
            if (count >= 1 && bb.remaining() >= 2) {
                data.put(CosemObject.DAYS_WITHOUT_COMMS_THRESHOLD.getObisCode(), readUint16(bb));
            }
        }

        // Tampering Attempts Threshold: array of 1 double-long-unsigned
        if (bb.remaining() >= 5) {
            int count = readUint8(bb);
            if (count >= 1 && bb.remaining() >= 4) {
                data.put(CosemObject.TAMPERING_ATTEMPTS_THRESHOLD.getObisCode(), readUint32(bb));
            }
        }

        // Leakage Test Parameters: structure (salva come raw)
        if (bb.remaining() >= 4) {
            byte[] leakParams = new byte[4];
            bb.get(leakParams);
            data.put(CosemObject.LEAKAGE_TEST_PARAMS.getObisCode(), leakParams);
        }

        log.debug("CF7 parsata: {} campi", data.getValues().size());
        return data;
    }

    /**
     * CF9 - Valve Management.
     *
     * Layout:
     * [0]     template_id                  unsigned       (1 byte) = 9
     * [1-9+]  Disconnect ctrl schedule     executed_script + execution_time (variabile)
     * [..]    Valve Enable Password        long-unsigned  (2 byte)
     * [..]    Opening Command Duration     long-unsigned  (2 byte)
     * [..]    spare                        octet-string   (1+ byte)
     *
     * La struttura e' complessa (script reference + time array).
     * Estraiamo i campi semplici alla fine.
     */
    static CompactFrameData parseCF9(byte[] buf) {
        CompactFrameData data = new CompactFrameData(9);
        ByteBuffer bb = wrap(buf);
        bb.get(); // skip template_id

        // executed_script: structure (octet-string 9 byte + long-unsigned 2 byte) = ~13 byte
        String schedObis = CosemObject.VALVE_SINGLE_ACTION_SCHEDULE.getObisCode();
        if (bb.remaining() >= 13) {
            byte[] scriptRef = new byte[13];
            bb.get(scriptRef);
            data.put(schedObis + "#script", scriptRef);
        }

        // execution_time: array of 1 (time 4 byte + date 5 byte) = ~12 byte con header
        if (bb.remaining() >= 12) {
            byte[] execTime = new byte[12];
            bb.get(execTime);
            data.put(schedObis + "#time", execTime);
        }

        if (bb.remaining() >= 2) data.put(CosemObject.VALVE_ENABLE_PASSWORD.getObisCode(), readUint16(bb));
        if (bb.remaining() >= 2) data.put(CosemObject.VALVE_OPENING_DURATION.getObisCode(), readUint16(bb));

        log.debug("CF9 parsata: {} campi", data.getValues().size());
        return data;
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
        data.put(OBIS_UNIX_TIME, unixTime);

        data.put(OBIS_PP4_STATUS, readUint16(bb));           // PP4 Network Status            
        data.put(OBIS_VALVE + "#output", readBoolean(bb));   // Valve output_state            
        data.put(OBIS_VALVE + "#control", readUint8(bb));    // Valve control_state           
        data.put(OBIS_METRO_EVT_CNT, readUint16(bb));        // Metrological Event Counter    
        data.put(OBIS_EVT_CNT, readUint16(bb));              // Event Counter                 
        data.put(OBIS_DAILY_DIAG, readUint16(bb));           // Daily Diagnostic              
        data.put(OBIS_CONV_VOL, readUint32(bb));             // Current Index Converted Volume
        data.put(OBIS_CONV_VOL_ALARM, readUint32(bb));       // Current Index Conv Vol Alarm  
        data.put(OBIS_BILLING_CNT, readUint8(bb));           // Billing Period Counter        

        if (bb.remaining() >= 4) {
            data.put(OBIS_MGMT_FC, readUint32(bb));          // Mgmt Frame Counter Online
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
        data.put(OBIS_UNIX_TIME, unixTime);

        data.put(OBIS_PP4_STATUS, readUint16(bb));
        data.put(OBIS_VALVE + "#output", readBoolean(bb));
        data.put(OBIS_VALVE + "#control", readUint8(bb));
        data.put(OBIS_METRO_EVT_CNT, readUint16(bb));
        data.put(OBIS_EVT_CNT, readUint16(bb));
        data.put(OBIS_DAILY_DIAG, readUint16(bb));
        data.put(OBIS_CONV_VOL, readUint32(bb));
        data.put(OBIS_CONV_VOL_ALARM, readUint32(bb));

        // Daily Load Profile (ultimi 3 entries) - salva come byte[] grezzo
        if (bb.remaining() >= 43) {
            byte[] dailyProfile = new byte[43];
            bb.get(dailyProfile);
            data.put(OBIS_DAILY_PROFILE, dailyProfile);
        }

        data.put(OBIS_BILLING_CNT, readUint8Safe(bb));

        if (bb.remaining() >= 4) {
            data.put(OBIS_MGMT_FC, readUint32(bb));
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
        data.put(OBIS_UNIX_TIME, unixTime);

        data.put(OBIS_PP4_STATUS, readUint16(bb));
        data.put(OBIS_VALVE + "#output", readBoolean(bb));
        data.put(OBIS_VALVE + "#control", readUint8(bb));
        data.put(OBIS_METRO_EVT_CNT, readUint16(bb));
        data.put(OBIS_EVT_CNT, readUint16(bb));
        data.put(OBIS_DAILY_DIAG, readUint16(bb));
        data.put(OBIS_CONV_VOL, readUint32(bb));
        data.put(OBIS_CONV_VOL_ALARM, readUint32(bb));

        // Daily Load Profile (ultimi 3 entries)
        if (bb.remaining() >= 43) {
            byte[] dailyProfile = new byte[43];
            bb.get(dailyProfile);
            data.put(OBIS_DAILY_PROFILE, dailyProfile);
        }

        data.put(OBIS_BILLING_CNT, readUint8Safe(bb));

        // Snapshot Period Data (ultimo entry)
        if (bb.remaining() >= 51) {
            byte[] snapshotData = new byte[51];
            bb.get(snapshotData);
            data.put(OBIS_SNAPSHOT, snapshotData);
        }

        if (bb.remaining() >= 4) {
            data.put(OBIS_MGMT_FC, readUint32(bb));
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

        data.put(OBIS_VALVE + "#output", readBoolean(bb));
        data.put(OBIS_VALVE + "#control", readUint8(bb));
        data.put(CosemObject.VALVE_CLOSURE_CAUSE.getObisCode(), readUint8(bb));

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
                data.put(CosemObject.LOGICAL_DEVICE_NAME.getObisCode(), new String(ldnBytes));
            }
        }

        if (bb.remaining() >= 4) data.put(CosemObject.MGMT_FRAME_COUNTER_ONLINE.getObisCode(), readUint32(bb));
        if (bb.remaining() >= 4) data.put(CosemObject.MGMT_FRAME_COUNTER_OFFLINE.getObisCode(), readUint32(bb));
        if (bb.remaining() >= 4) data.put(CosemObject.GA_FRAME_COUNTER.getObisCode(), readUint32(bb));
        if (bb.remaining() >= 4) data.put(CosemObject.IM_FRAME_COUNTER.getObisCode(), readUint32(bb));

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
