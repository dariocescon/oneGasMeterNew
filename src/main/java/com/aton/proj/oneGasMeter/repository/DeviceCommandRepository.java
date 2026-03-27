package com.aton.proj.oneGasMeter.repository;

import com.aton.proj.oneGasMeter.entity.CommandStatus;
import com.aton.proj.oneGasMeter.entity.DeviceCommand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository per l'accesso ai comandi dispositivo.
 */
@Repository
public interface DeviceCommandRepository extends JpaRepository<DeviceCommand, Long> {

    /** Trova i comandi per un contatore in uno specifico stato */
    List<DeviceCommand> findBySerialNumberAndStatus(String serialNumber, CommandStatus status);

    /** Trova tutti i comandi per un contatore */
    List<DeviceCommand> findBySerialNumber(String serialNumber);
}
