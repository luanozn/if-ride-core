package com.ifride.core.shared.services;

import org.cryptomator.siv.SivMode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

@Service
public class DeterministicEncryptorService {

    private final SivMode sivMode;
    private final SecretKey secretKey;

    public DeterministicEncryptorService(@Value("${app.deterministic.aes-key}") String rawKey) throws Exception {
        if (rawKey == null || rawKey.isBlank()) {
            throw new IllegalStateException(
                    "app.deterministic.aes-key não configurada. " +
                            "Em dev, defina em application-dev.properties; " +
                            "em prod, configure no Parameter Store.");
        }

        byte[] keyBytes = MessageDigest.getInstance("SHA-256")
                .digest(rawKey.getBytes(StandardCharsets.UTF_8));

        this.secretKey = new SecretKeySpec(keyBytes, "AES");
        this.sivMode = new SivMode();
    }

    public String encrypt(String value) {
        try {
            byte[] plaintext = value.getBytes(StandardCharsets.UTF_8);
            byte[] ciphertext = sivMode.encrypt(secretKey, plaintext);
            return Base64.getEncoder().encodeToString(ciphertext);
        } catch (Exception e) {
            throw new IllegalStateException("Erro ao criptografar o valor", e);
        }
    }

    public String decrypt(String ciphertext) {
        try {
            byte[] cipherBytes = Base64.getDecoder().decode(ciphertext);
            byte[] plaintext = sivMode.decrypt(secretKey, cipherBytes);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Erro ao descriptografar o valor (ou dado corrompido)", e);
        }
    }
}