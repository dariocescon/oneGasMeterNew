package com.aton.proj.oneGasMeter.repository;

import com.aton.proj.oneGasMeter.entity.SessionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository per il log sessioni (session_log).
 */
@Repository
public interface SessionLogRepository extends JpaRepository<SessionLog, Long> {

    /** Trova le sessioni per numero seriale, ordinate per data decrescente. */
    List<SessionLog> findBySerialNumberOrderByStartedAtDesc(String serialNumber);
}
