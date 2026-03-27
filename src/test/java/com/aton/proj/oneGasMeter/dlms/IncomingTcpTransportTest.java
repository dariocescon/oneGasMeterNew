package com.aton.proj.oneGasMeter.dlms;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test unitari per IncomingTcpTransport.
 */
class IncomingTcpTransportTest {

    @Test
    void receiveReadsWrapperFrame() throws IOException {
        // Prepara un frame WRAPPER: header 8 bytes + payload 3 bytes
        // Version=0x0001, Source=0x0010, Dest=0x0001, Length=0x0003
        byte[] wrapperFrame = new byte[]{
                0x00, 0x01,  // version
                0x00, 0x10,  // source address
                0x00, 0x01,  // destination address
                0x00, 0x03,  // length = 3
                0x41, 0x42, 0x43  // payload "ABC"
        };

        Socket mockSocket = createMockSocket(wrapperFrame, new ByteArrayOutputStream());
        IncomingTcpTransport transport = new IncomingTcpTransport(mockSocket, 5000);

        byte[] received = transport.receive();
        assertEquals(11, received.length); // 8 header + 3 payload
        assertEquals(0x41, received[8]);
        assertEquals(0x42, received[9]);
        assertEquals(0x43, received[10]);
    }

    @Test
    void sendWritesToOutputStream() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Socket mockSocket = createMockSocket(new byte[0], outputStream);
        IncomingTcpTransport transport = new IncomingTcpTransport(mockSocket, 5000);

        byte[] data = new byte[]{0x01, 0x02, 0x03};
        transport.send(data);

        assertArrayEquals(data, outputStream.toByteArray());
    }

    @Test
    void closeClosesSocket() throws IOException {
        Socket mockSocket = createMockSocket(new byte[0], new ByteArrayOutputStream());
        when(mockSocket.isClosed()).thenReturn(false);
        IncomingTcpTransport transport = new IncomingTcpTransport(mockSocket, 5000);

        transport.close();
        verify(mockSocket).close();
    }

    @Test
    void isConnectedReturnsTrueWhenSocketIsOpen() throws IOException {
        Socket mockSocket = createMockSocket(new byte[0], new ByteArrayOutputStream());
        when(mockSocket.isClosed()).thenReturn(false);
        when(mockSocket.isConnected()).thenReturn(true);
        IncomingTcpTransport transport = new IncomingTcpTransport(mockSocket, 5000);

        assertTrue(transport.isConnected());
    }

    @Test
    void isConnectedReturnsFalseWhenSocketIsClosed() throws IOException {
        Socket mockSocket = createMockSocket(new byte[0], new ByteArrayOutputStream());
        when(mockSocket.isClosed()).thenReturn(true);
        IncomingTcpTransport transport = new IncomingTcpTransport(mockSocket, 5000);

        assertFalse(transport.isConnected());
    }

    @Test
    void receiveThrowsOnInvalidPayloadLength() throws IOException {
        // Frame con lunghezza 0
        byte[] wrapperFrame = new byte[]{
                0x00, 0x01, 0x00, 0x10, 0x00, 0x01,
                0x00, 0x00  // length = 0
        };

        Socket mockSocket = createMockSocket(wrapperFrame, new ByteArrayOutputStream());
        IncomingTcpTransport transport = new IncomingTcpTransport(mockSocket, 5000);

        assertThrows(IOException.class, transport::receive);
    }

    private Socket createMockSocket(byte[] inputData, ByteArrayOutputStream outputStream) throws IOException {
        Socket socket = mock(Socket.class);
        when(socket.getInputStream()).thenReturn(new ByteArrayInputStream(inputData));
        when(socket.getOutputStream()).thenReturn(outputStream);
        return socket;
    }
}
