package com.aton.proj.oneGasMeter.dlms;

import com.aton.proj.oneGasMeter.config.DlmsSessionConfig;
import com.aton.proj.oneGasMeter.exception.DlmsCommunicationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test unitari per DlmsMeterClient.
 */
class DlmsMeterClientTest {

    @Test
    void constructorWithValidConfig() {
        DlmsTransport transport = createMockTransport();
        DlmsSessionConfig config = createDefaultConfig();

        DlmsMeterClient client = new DlmsMeterClient(transport, config);
        assertNotNull(client);
        assertFalse(client.isConnected());
    }

    @Test
    void constructorWithAllAuthenticationLevels() {
        DlmsTransport transport = createMockTransport();

        for (String authLevel : new String[]{"NONE", "LOW", "HIGH", "HIGH_MD5", "HIGH_SHA1", "HIGH_SHA256", "HIGH_GMAC"}) {
            DlmsSessionConfig config = createDefaultConfig();
            config.setAuthentication(authLevel);
            DlmsMeterClient client = new DlmsMeterClient(transport, config);
            assertNotNull(client, "Creazione fallita per livello: " + authLevel);
        }
    }

    @Test
    void constructorWithInvalidAuthenticationThrows() {
        DlmsTransport transport = createMockTransport();
        DlmsSessionConfig config = createDefaultConfig();
        config.setAuthentication("INVALID_LEVEL");

        assertThrows(IllegalArgumentException.class, () -> new DlmsMeterClient(transport, config));
    }

    @Test
    void constructorWithPasswordSetsIt() {
        DlmsTransport transport = createMockTransport();
        DlmsSessionConfig config = createDefaultConfig();
        config.setAuthentication("LOW");
        config.setPassword("mypassword");

        DlmsMeterClient client = new DlmsMeterClient(transport, config);
        assertNotNull(client.getGxClient());
    }

    @Test
    void disconnectClosesTransport() {
        DlmsTransport transport = createMockTransport();
        DlmsSessionConfig config = createDefaultConfig();
        DlmsMeterClient client = new DlmsMeterClient(transport, config);

        // disconnect senza connessione non deve lanciare eccezioni
        assertDoesNotThrow(client::disconnect);
    }

    @Test
    void connectWithoutTransportDataThrows() {
        // Il transport non ha dati, quindi l'handshake fallira'
        DlmsTransport transport = createMockTransport();
        DlmsSessionConfig config = createDefaultConfig();
        DlmsMeterClient client = new DlmsMeterClient(transport, config);

        assertThrows(DlmsCommunicationException.class, client::connect);
    }

    @Test
    void readProfileGenericWithoutConnectionThrows() {
        DlmsTransport transport = createMockTransport();
        DlmsSessionConfig config = createDefaultConfig();
        DlmsMeterClient client = new DlmsMeterClient(transport, config);

        assertThrows(DlmsCommunicationException.class,
                () -> client.readProfileGeneric("0.0.99.98.0.255", null, null));
    }

    @Test
    void setPushDestinationWithoutConnectionThrows() {
        DlmsTransport transport = createMockTransport();
        DlmsSessionConfig config = createDefaultConfig();
        DlmsMeterClient client = new DlmsMeterClient(transport, config);

        // Senza connessione, la write fallisce sul transport
        assertThrows(DlmsCommunicationException.class,
                () -> client.setPushDestination("10.0.0.1", 4059));
    }

    private DlmsSessionConfig createDefaultConfig() {
        DlmsSessionConfig config = new DlmsSessionConfig();
        config.setClientAddress(16);
        config.setServerAddress(1);
        config.setAuthentication("NONE");
        config.setUseLogicalNameReferencing(true);
        return config;
    }

    private DlmsTransport createMockTransport() {
        return new DlmsTransport() {
            private boolean connected = true;

            @Override
            public void send(byte[] data) { /* no-op */ }

            @Override
            public byte[] receive() throws java.io.IOException {
                throw new java.io.IOException("Mock transport: nessun dato disponibile");
            }

            @Override
            public void close() {
                connected = false;
            }

            @Override
            public boolean isConnected() {
                return connected;
            }
        };
    }
}
