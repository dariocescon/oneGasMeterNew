package com.aton.proj.oneGasMeter.repository;

import com.aton.proj.oneGasMeter.entity.DeviceRegistry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository per l'anagrafica dispositivi (device_registry).
 * Chiave primaria: serial_number (String).
 */
@Repository
public interface DeviceRegistryRepository extends JpaRepository<DeviceRegistry, String> {
}
