package com.aton.proj.oneGasMeter.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;

/**
 * Servizio per cifrare e decifrare le chiavi DLMS memorizzate nel database.
 *
 * Le chiavi DLMS (encryption_key, authentication_key, master_key) vengono
 * cifrate con AES-256-GCM prima di essere salvate in device_registry.
 * La master password e' configurata tramite variabile d'ambiente.
 *
 * Formato dei dati cifrati: [IV 12 byte] + [ciphertext + GCM tag]
 */
@Service
public class KeyEncryptionService {

    private static final Logger log = LoggerFactory.getLogger(KeyEncryptionService.class);

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    private final SecretKey masterKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public KeyEncryptionService(@Value("${security.key-master-password}") String masterPassword) {
        this.masterKey = deriveMasterKey(masterPassword);
        log.info("KeyEncryptionService inizializzato");
    }

    /**
     * Cifra una chiave DLMS (16 byte) per il salvataggio in database.
     *
     * @param plainKey chiave in chiaro (tipicamente 16 byte per AES-128)
     * @return dati cifrati: IV (12 byte) + ciphertext + GCM tag
     */
    public byte[] encrypt(byte[] plainKey) {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, masterKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] ciphertext = cipher.doFinal(plainKey);

            // Concatena IV + ciphertext
            byte[] result = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(ciphertext, 0, result, iv.length, ciphertext.length);
            return result;

        } catch (Exception e) {
            throw new RuntimeException("Errore cifratura chiave DLMS", e);
        }
    }

    /**
     * Decifra una chiave DLMS dal formato database.
     *
     * @param encryptedData dati cifrati (IV + ciphertext + GCM tag)
     * @return chiave in chiaro
     */
    public byte[] decrypt(byte[] encryptedData) {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(encryptedData, 0, iv, 0, iv.length);

            byte[] ciphertext = new byte[encryptedData.length - iv.length];
            System.arraycopy(encryptedData, iv.length, ciphertext, 0, ciphertext.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, masterKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            return cipher.doFinal(ciphertext);

        } catch (Exception e) {
            throw new RuntimeException("Errore decifratura chiave DLMS", e);
        }
    }

    /**
     * Deriva una chiave AES-256 dalla master password usando SHA-256.
     */
    private static SecretKey deriveMasterKey(String password) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = sha256.digest(password.getBytes());
            return new SecretKeySpec(keyBytes, "AES");
        } catch (Exception e) {
            throw new RuntimeException("Errore derivazione master key", e);
        }
    }
}
