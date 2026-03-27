package com.aton.proj.oneGasMeter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configurazione della sessione DLMS/COSEM.
 * Valori letti da application.properties con prefisso "dlms".
 */
@Component
@ConfigurationProperties(prefix = "dlms")
public class DlmsSessionConfig {

    /** Indirizzo client DLMS (tipicamente 16 per il client) */
    private int clientAddress = 16;

    /** Indirizzo server DLMS (tipicamente 1 per il contatore) */
    private int serverAddress = 1;

    /**
     * Livello di autenticazione DLMS.
     * Valori supportati: NONE, LOW, HIGH_MD5, HIGH_SHA1, HIGH_SHA256, HIGH_GMAC
     */
    private String authentication = "NONE";

    /** Password per autenticazione LOW o secret per HIGH */
    private String password = "";

    /** Usa Logical Name Referencing (true) o Short Name Referencing (false) */
    private boolean useLogicalNameReferencing = true;

    public int getClientAddress() { return clientAddress; }
    public void setClientAddress(int clientAddress) { this.clientAddress = clientAddress; }

    public int getServerAddress() { return serverAddress; }
    public void setServerAddress(int serverAddress) { this.serverAddress = serverAddress; }

    public String getAuthentication() { return authentication; }
    public void setAuthentication(String authentication) { this.authentication = authentication; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public boolean isUseLogicalNameReferencing() { return useLogicalNameReferencing; }
    public void setUseLogicalNameReferencing(boolean useLogicalNameReferencing) {
        this.useLogicalNameReferencing = useLogicalNameReferencing;
    }
}
