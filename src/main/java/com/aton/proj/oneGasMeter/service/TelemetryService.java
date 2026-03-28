package com.aton.proj.oneGasMeter.service;

import com.aton.proj.oneGasMeter.entity.TelemetryData;
import com.aton.proj.oneGasMeter.repository.TelemetryDataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HexFormat;
import java.util.List;

/**
 * Servizio per il salvataggio e la consultazione dei dati di telemetria.
 */
@Service
public class TelemetryService {

    private static final Logger log = LoggerFactory.getLogger(TelemetryService.class);

    private final TelemetryDataRepository repository;

    public TelemetryService(TelemetryDataRepository repository) {
        this.repository = repository;
    }

    /**
     * Salva una singola lettura di telemetria.
     *
     * @param serialNumber  numero seriale del contatore
     * @param meterIp       indirizzo IP del contatore
     * @param sessionId     UUID della sessione
     * @param obisCode      codice OBIS dell'oggetto letto
     * @param classId       Class ID COSEM
     * @param rawValue      valore grezzo
     * @param scaler        scalatore (esponente base 10)
     * @param unit          unita' di misura
     * @param meterTimestamp timestamp dal contatore
     * @return l'entita' salvata
     */
    public TelemetryData save(String serialNumber, String meterIp, String sessionId,
                               String obisCode, int classId, Object rawValue,
                               double scaler, String unit, Instant meterTimestamp) {
        TelemetryData data = new TelemetryData();
        data.setSerialNumber(serialNumber);
        data.setMeterIp(meterIp);
        data.setSessionId(sessionId);
        data.setObisCode(obisCode);
        data.setClassId(classId);
        data.setRawValue(formatRawValue(rawValue));
        data.setScaler(scaler);
        data.setUnit(unit);
        data.setScaledValue(computeScaledValue(rawValue, scaler));
        data.setMeterTimestamp(meterTimestamp);
        data.setReceivedAt(Instant.now());

        TelemetryData saved = repository.save(data);
        log.debug("Salvata lettura: {} OBIS={} valore={}", serialNumber, obisCode, saved.getScaledValue());
        return saved;
    }

    /**
     * Trova tutte le letture di una sessione.
     */
    public List<TelemetryData> findBySessionId(String sessionId) {
        return repository.findBySessionId(sessionId);
    }

    /**
     * Trova tutte le letture di un contatore.
     */
    public List<TelemetryData> findBySerialNumber(String serialNumber) {
        return repository.findBySerialNumber(serialNumber);
    }

    /**
     * Converte il valore grezzo in stringa leggibile.
     * Gestisce byte[] (comune in DLMS) convertendolo in hex.
     */
    private String formatRawValue(Object rawValue) {
        if (rawValue == null) {
            return null;
        }
        if (rawValue instanceof byte[] bytes) {
            return HexFormat.of().formatHex(bytes);
        }
        return String.valueOf(rawValue);
    }

    /**
     * Calcola il valore scalato: rawValue * 10^scaler.
     * Restituisce null se il valore grezzo non e' numerico.
     */
    private Double computeScaledValue(Object rawValue, double scaler) {
        if (rawValue == null) {
            return null;
        }
        try {
            double numericValue = Double.parseDouble(String.valueOf(rawValue));
            return numericValue * Math.pow(10, scaler);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
