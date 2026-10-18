package com.ifride.core.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

@Service
public class DeterministicEncryptorService {

    private static final String AES = "AES/CBC/PKCS5Padding";
    private static final IvParameterSpec FIXED_IV = new IvParameterSpec(new byte[16]);

    private final SecretKey key;

    public DeterministicEncryptorService(@Value("${app.cpf.aes-key}") String rawKey) throws Exception {
        if (rawKey == null || rawKey.isBlank()) {
            throw new IllegalStateException(
                    "app.cpf.aes-key não configurada. " +
                            "Em dev, defina em application-dev.properties; " +
                            "em prod, configure no Parameter Store.");
        }
        byte[] keyBytes = MessageDigest.getInstance("SHA-256")
                .digest(rawKey.getBytes(StandardCharsets.UTF_8));
        this.key = new SecretKeySpec(keyBytes, "AES");
    }

    public String encrypt(String value) {
        try {
            Cipher c = Cipher.getInstance(AES);
            c.init(Cipher.ENCRYPT_MODE, key, FIXED_IV);
            byte[] encrypted = c.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new IllegalStateException("Erro ao criptografar o valor", e);
        }
    }

    public String decrypt(String ciphertext) {
        try {
            byte[] encrypted = Base64.getDecoder().decode(ciphertext);
            Cipher c = Cipher.getInstance(AES);
            c.init(Cipher.DECRYPT_MODE, key, FIXED_IV);
            byte[] decrypted = c.doFinal(encrypted);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Erro ao descriptografar CPF", e);
        }
    }
}