package com.ifride.core.service;

import com.ifride.core.shared.services.DeterministicEncryptorService;
import com.ifride.core.shared.services.RandomizedEncryptorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EncryptorTest {

    private final String rawKey = "my-super-secret-test-key-for-tcc-123456";

    @Nested
    @DisplayName("Criptografia Determinística (CPF/CNH)")
    class DeterministicEncryption {

        private DeterministicEncryptorService service;

        @BeforeEach
        void setUp() throws Exception {
            service = new DeterministicEncryptorService(rawKey);
        }

        @Test
        @DisplayName("Deve criptografar e descriptografar mantendo o valor original")
        void shouldEncryptAndDecryptSuccessfully() {
            String originalValue = "12345678901";

            String encrypted = service.encrypt(originalValue);
            String decrypted = service.decrypt(encrypted);

            assertThat(encrypted).isNotEqualTo(originalValue);
            assertThat(decrypted).isEqualTo(originalValue);
        }

        @Test
        @DisplayName("Deve gerar o mesmo hash para o mesmo valor (permitindo busca por igualdade)")
        void shouldGenerateSameHashForSameValue() {
            String cpf = "12345678901";

            String encrypted1 = service.encrypt(cpf);
            String encrypted2 = service.encrypt(cpf);

            assertThat(encrypted1).isEqualTo(encrypted2);
        }
    }

    @Nested
    @DisplayName("Criptografia Randomizada (Código de Segurança)")
    class RandomizedEncryption {

        private RandomizedEncryptorService service;

        @BeforeEach
        void setUp() throws Exception {
            service = new RandomizedEncryptorService(rawKey);
        }

        @Test
        @DisplayName("Deve criptografar e descriptografar mantendo o valor original")
        void shouldEncryptAndDecryptSuccessfully() {
            String originalValue = "1234";

            String encrypted = service.encrypt(originalValue);
            String decrypted = service.decrypt(encrypted);

            assertThat(encrypted).isNotEqualTo(originalValue);
            assertThat(decrypted).isEqualTo(originalValue);
        }

        @Test
        @DisplayName("Deve gerar hashes diferentes para o mesmo valor (garantindo segurança e IV aleatório)")
        void shouldGenerateDifferentHashesForSameValue() {
            String securityCode = "1234";

            String encrypted1 = service.encrypt(securityCode);
            String encrypted2 = service.encrypt(securityCode);

            assertThat(encrypted1).isNotEqualTo(encrypted2);
        }
    }

    @Nested
    @DisplayName("Proteção contra Adulteração (Tampering)")
    class TamperProtection {

        private RandomizedEncryptorService service;

        @BeforeEach
        void setUp() throws Exception {
            service = new RandomizedEncryptorService(rawKey);
        }

        @Test
        @DisplayName("Deve lançar exceção se o texto cifrado for alterado (Bit-Flipping)")
        void shouldThrowExceptionWhenCiphertextIsTampered() {
            String originalValue = "1234";
            String encryptedBase64 = service.encrypt(originalValue);

            // 1. Transforma o Base64 de volta para bytes para manipular
            byte[] cipherBytes = java.util.Base64.getDecoder().decode(encryptedBase64);

            // 2. Altera 1 bit no meio do texto cifrado (após o IV de 12 bytes)
            int tamperedIndex = 15;
            cipherBytes[tamperedIndex] = (byte) (cipherBytes[tamperedIndex] ^ 1); // Operador XOR inverte o bit

            // 3. Transforma de volta para Base64
            String tamperedBase64 = java.util.Base64.getEncoder().encodeToString(cipherBytes);

            // 4. Tenta descriptografar e espera o erro
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.decrypt(tamperedBase64))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Erro ao descriptografar mensagem (ou dado corrompido)");
        }

        @Test
        @DisplayName("Deve lançar exceção se a Tag de autenticação for alterada")
        void shouldThrowExceptionWhenTagIsTampered() {
            String originalValue = "1234";
            String encryptedBase64 = service.encrypt(originalValue);

            byte[] cipherBytes = java.util.Base64.getDecoder().decode(encryptedBase64);

            int lastIndex = cipherBytes.length - 1;
            cipherBytes[lastIndex] = (byte) (cipherBytes[lastIndex] + 1);

            String tamperedBase64 = java.util.Base64.getEncoder().encodeToString(cipherBytes);

            org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.decrypt(tamperedBase64))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Erro ao descriptografar mensagem (ou dado corrompido)");
        }

        @Test
        @DisplayName("Deve lançar exceção se o IV for alterado")
        void shouldThrowExceptionWhenIvIsTampered() {
            String encryptedBase64 = service.encrypt("1234");
            byte[] cipherBytes = java.util.Base64.getDecoder().decode(encryptedBase64);

            cipherBytes[0] = (byte) (cipherBytes[0] + 1);

            String tamperedBase64 = java.util.Base64.getEncoder().encodeToString(cipherBytes);

            org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.decrypt(tamperedBase64))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Erro ao descriptografar mensagem (ou dado corrompido)");
        }
    }
}