package com.aton.proj.oneGasMeter.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test unitari per KeyEncryptionService.
 * Verifica il round-trip cifratura/decifratura delle chiavi DLMS.
 */
class KeyEncryptionServiceTest {

    private final KeyEncryptionService service = new KeyEncryptionService("testMasterPassword123");

    @Test
    void encryptDecryptRoundTrip() {
        // Chiave DLMS tipica: 16 byte (AES-128)
        byte[] originalKey = {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08,
                              0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F, 0x10};

        byte[] encrypted = service.encrypt(originalKey);
        byte[] decrypted = service.decrypt(encrypted);

        assertArrayEquals(originalKey, decrypted);
    }

    @Test
    void encryptedDataIsLongerThanOriginal() {
        byte[] originalKey = new byte[16];
        byte[] encrypted = service.encrypt(originalKey);

        // encrypted = IV(12) + ciphertext(16) + GCM tag(16) = 44 byte
        assertTrue(encrypted.length > originalKey.length);
    }

    @Test
    void twoEncryptionsProduceDifferentResults() {
        byte[] originalKey = {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08,
                              0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F, 0x10};

        byte[] encrypted1 = service.encrypt(originalKey);
        byte[] encrypted2 = service.encrypt(originalKey);

        // IV diverso ogni volta -> risultati diversi
        assertFalse(java.util.Arrays.equals(encrypted1, encrypted2));
    }

    @Test
    void decryptWithWrongPasswordFails() {
        KeyEncryptionService otherService = new KeyEncryptionService("wrongPassword");
        byte[] originalKey = new byte[16];
        byte[] encrypted = service.encrypt(originalKey);

        assertThrows(RuntimeException.class, () -> otherService.decrypt(encrypted));
    }

    @Test
    void decryptWithCorruptedDataFails() {
        byte[] originalKey = new byte[16];
        byte[] encrypted = service.encrypt(originalKey);

        // Corrompi un byte
        encrypted[encrypted.length - 1] ^= 0xFF;

        assertThrows(RuntimeException.class, () -> service.decrypt(encrypted));
    }
}
