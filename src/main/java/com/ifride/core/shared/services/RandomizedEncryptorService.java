package com.ifride.core.shared.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class RandomizedEncryptorService {

    private static final String AES = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;

    private final SecretKey key;
    private final SecureRandom secureRandom;

    public RandomizedEncryptorService(@Value("${app.randomized.aes-key}") String rawKey) throws Exception {
        if (rawKey == null || rawKey.isBlank()) {
            throw new IllegalStateException("app.randomized.aes-key não configurada.");
        }
        byte[] keyBytes = MessageDigest.getInstance("SHA-256")
                .digest(rawKey.getBytes(StandardCharsets.UTF_8));
        this.key = new SecretKeySpec(keyBytes, "AES");
        this.secureRandom = new SecureRandom();
    }

    public String encrypt(String value) {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher c = Cipher.getInstance(AES);
            c.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] ciphertext = c.doFinal(value.getBytes(StandardCharsets.UTF_8));

            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + ciphertext.length);
            byteBuffer.put(iv);
            byteBuffer.put(ciphertext);

            return Base64.getEncoder().encodeToString(byteBuffer.array());
        } catch (Exception e) {
            throw new IllegalStateException("Erro ao criptografar mensagem", e);
        }
    }

    public String decrypt(String base64Ciphertext) {
        try {
            byte[] cipherMessage = Base64.getDecoder().decode(base64Ciphertext);
            ByteBuffer byteBuffer = ByteBuffer.wrap(cipherMessage);

            byte[] iv = new byte[GCM_IV_LENGTH];
            byteBuffer.get(iv);

            byte[] ciphertext = new byte[byteBuffer.remaining()];
            byteBuffer.get(ciphertext);

            Cipher c = Cipher.getInstance(AES);
            c.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            byte[] decrypted = c.doFinal(ciphertext);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Erro ao descriptografar mensagem (ou dado corrompido)", e);
        }
    }
}