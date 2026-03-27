package com.aton.proj.oneGasMeter.dlms;

import java.io.IOException;

/**
 * Interfaccia per il trasporto dei frame DLMS.
 * Astrae il canale di comunicazione (TCP WRAPPER, HDLC, ecc.).
 */
public interface DlmsTransport {

    /**
     * Invia dati al dispositivo.
     *
     * @param data bytes da inviare
     * @throws IOException in caso di errore di I/O
     */
    void send(byte[] data) throws IOException;

    /**
     * Riceve dati dal dispositivo.
     *
     * @return bytes ricevuti
     * @throws IOException in caso di errore di I/O o timeout
     */
    byte[] receive() throws IOException;

    /**
     * Chiude il trasporto e rilascia le risorse.
     */
    void close();

    /**
     * Verifica se il trasporto e' ancora connesso.
     */
    boolean isConnected();
}
