package com.aton.proj.oneGasMeter.dlms;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Trasporto DLMS su TCP con framing WRAPPER (IEC 62056-47).
 * Utilizzato per connessioni TCP in ingresso dai contatori gas.
 *
 * Formato frame WRAPPER:
 *   [0-1] Version (0x0001)
 *   [2-3] Source address
 *   [4-5] Destination address
 *   [6-7] Length (payload length)
 *   [8..] Payload (DLMS APDU)
 */
public class IncomingTcpTransport implements DlmsTransport {

    private static final Logger log = LoggerFactory.getLogger(IncomingTcpTransport.class);
    private static final int WRAPPER_HEADER_SIZE = 8;
    private static final int MAX_PAYLOAD_SIZE = 8192;

    private final Socket socket;
    private final InputStream inputStream;
    private final OutputStream outputStream;

    /**
     * Crea un trasporto WRAPPER su un socket TCP gia' connesso.
     *
     * @param socket    socket TCP accettato dal server
     * @param timeoutMs timeout di lettura in millisecondi
     */
    public IncomingTcpTransport(Socket socket, int timeoutMs) throws IOException {
        this.socket = socket;
        this.socket.setSoTimeout(timeoutMs);
        this.inputStream = socket.getInputStream();
        this.outputStream = socket.getOutputStream();
    }

    @Override
    public void send(byte[] data) throws IOException {
        outputStream.write(data);
        outputStream.flush();
    }

    @Override
    public byte[] receive() throws IOException {
        // Leggi l'header WRAPPER (8 bytes)
        byte[] header = readExactly(WRAPPER_HEADER_SIZE);

        // Estrai la lunghezza del payload dai bytes 6-7
        int payloadLength = ((header[6] & 0xFF) << 8) | (header[7] & 0xFF);

        if (payloadLength <= 0 || payloadLength > MAX_PAYLOAD_SIZE) {
            throw new IOException("Lunghezza payload WRAPPER non valida: " + payloadLength);
        }

        // Leggi il payload
        byte[] payload = readExactly(payloadLength);

        // Ricomponi il frame completo (header + payload)
        byte[] fullFrame = new byte[WRAPPER_HEADER_SIZE + payloadLength];
        System.arraycopy(header, 0, fullFrame, 0, WRAPPER_HEADER_SIZE);
        System.arraycopy(payload, 0, fullFrame, WRAPPER_HEADER_SIZE, payloadLength);

        log.trace("Ricevuto frame WRAPPER: {} bytes (payload: {} bytes)", fullFrame.length, payloadLength);
        return fullFrame;
    }

    @Override
    public void close() {
        try {
            if (!socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            log.warn("Errore durante la chiusura del socket: {}", e.getMessage());
        }
    }

    @Override
    public boolean isConnected() {
        return !socket.isClosed() && socket.isConnected();
    }

    /**
     * Legge esattamente n bytes dallo stream, attendendo se necessario.
     *
     * @param n numero di bytes da leggere
     * @return array di n bytes
     * @throws IOException se lo stream si chiude prima di aver letto tutti i bytes
     */
    private byte[] readExactly(int n) throws IOException {
        byte[] buffer = new byte[n];
        int totalRead = 0;
        while (totalRead < n) {
            int read = inputStream.read(buffer, totalRead, n - totalRead);
            if (read == -1) {
                throw new IOException("Stream chiuso dopo aver letto " + totalRead + " di " + n + " bytes");
            }
            totalRead += read;
        }
        return buffer;
    }
}
