package com.aton.proj.oneGasMeter.service;

import com.aton.proj.oneGasMeter.entity.CommandStatus;
import com.aton.proj.oneGasMeter.entity.DeviceCommand;
import com.aton.proj.oneGasMeter.repository.DeviceCommandRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Servizio per la gestione dei comandi da inviare ai contatori.
 */
@Service
public class CommandService {

    private static final Logger log = LoggerFactory.getLogger(CommandService.class);

    private final DeviceCommandRepository repository;

    public CommandService(DeviceCommandRepository repository) {
        this.repository = repository;
    }

    /**
     * Recupera tutti i comandi in stato PENDING per un contatore.
     */
    public List<DeviceCommand> getPendingCommands(String serialNumber) {
        return repository.findBySerialNumberAndStatus(serialNumber, CommandStatus.PENDING);
    }

    /**
     * Segna un comando come in esecuzione.
     */
    @Transactional
    public void markInProgress(DeviceCommand command) {
        command.setStatus(CommandStatus.IN_PROGRESS);
        repository.save(command);
        log.debug("Comando {} in esecuzione per {}", command.getCommandType(), command.getSerialNumber());
    }

    /**
     * Segna un comando come completato con successo.
     */
    @Transactional
    public void markDone(DeviceCommand command) {
        command.setStatus(CommandStatus.DONE);
        command.setExecutedAt(Instant.now());
        repository.save(command);
        log.info("Comando {} completato per {}", command.getCommandType(), command.getSerialNumber());
    }

    /**
     * Segna un comando come fallito.
     */
    @Transactional
    public void markFailed(DeviceCommand command, String errorMessage) {
        command.setStatus(CommandStatus.FAILED);
        command.setExecutedAt(Instant.now());
        command.setErrorMessage(errorMessage);
        repository.save(command);
        log.warn("Comando {} fallito per {}: {}", command.getCommandType(), command.getSerialNumber(), errorMessage);
    }
}
