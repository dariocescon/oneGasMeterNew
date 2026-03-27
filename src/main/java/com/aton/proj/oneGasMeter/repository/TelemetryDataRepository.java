package com.aton.proj.oneGasMeter.repository;

import com.aton.proj.oneGasMeter.entity.TelemetryData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository per l'accesso ai dati di telemetria.
 */
@Repository
public interface TelemetryDataRepository extends JpaRepository<TelemetryData, Long> {

    /** Trova tutte le letture di un contatore per numero seriale */
    List<TelemetryData> findBySerialNumber(String serialNumber);

    /** Trova tutte le letture di una sessione */
    List<TelemetryData> findBySessionId(String sessionId);

    /** Trova letture di un contatore per uno specifico codice OBIS */
    List<TelemetryData> findBySerialNumberAndObisCode(String serialNumber, String obisCode);
}
