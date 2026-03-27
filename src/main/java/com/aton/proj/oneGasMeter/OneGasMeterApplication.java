package com.aton.proj.oneGasMeter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto di ingresso dell'applicazione OneGasMeter.
 * Server TCP per la raccolta teleletture da contatori gas italiani
 * tramite protocollo DLMS/COSEM.
 */
@SpringBootApplication
public class OneGasMeterApplication {

    public static void main(String[] args) {
        SpringApplication.run(OneGasMeterApplication.class, args);
    }
}
