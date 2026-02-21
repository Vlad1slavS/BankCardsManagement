package com.example.bankcards.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Утилита шифрования AES-GCM для номеров карт.
 * Использует случайный 12-байтовый вектор инициализации (IV)
 *
 * @author Владислав Степанов
 */
@Slf4j
@Component
public class CardEncryptionUtil {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    private final transient SecretKey secretKey;

    public CardEncryptionUtil(@Value("${app.encryption.secret-key}") String rawKey) {
        this.secretKey = deriveKey(rawKey);
    }

    /**
     * Получить зашифрованный plaintext
     *
     * @param plaintext - данные для шифрования
     * @return зашифрованная строка
     */
    public String encrypt(String plaintext) {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            ByteBuffer buffer = ByteBuffer.allocate(iv.length + encrypted.length);
            buffer.put(iv);
            buffer.put(encrypted);
            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception e) {
            log.error("Ошибка при шифровании номера", e);
            throw new IllegalStateException("Ошибка при шифровании номера", e);
        }
    }

    /**
     * Получить дешифрованный plaintext
     *
     * @param ciphertext - данные для дешифрования
     * @return дешифрованная строка
     */
    public String decrypt(String ciphertext) {
        try {
            byte[] decoded = Base64.getDecoder().decode(ciphertext);
            ByteBuffer buffer = ByteBuffer.wrap(decoded);

            byte[] iv = new byte[GCM_IV_LENGTH];
            buffer.get(iv);
            byte[] encryptedData = new byte[buffer.remaining()];
            buffer.get(encryptedData);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            return new String(cipher.doFinal(encryptedData), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Ошибка при дешифровании номера", e);
            throw new IllegalStateException("Ошибка при дешифровании номера", e);
        }
    }

    /**
     * Получения маски от номера карты
     *
     * @param cardNumber - номер карты
     * @return Маска, формата (**** **** **** 1111)
     */
    public String mask(String cardNumber) {
        String digits = cardNumber.replaceAll("[^0-9]", "");
        if (digits.length() < 4) {
            throw new IllegalArgumentException("Длина номера карты слишком короткая");
        }
        String last4 = digits.substring(digits.length() - 4);
        return "**** **** **** " + last4;
    }

    private SecretKey deriveKey(String rawKey) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = sha.digest(rawKey.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(keyBytes, "AES");
        } catch (Exception e) {
            throw new IllegalStateException("Не удалось получить ключ шифрования", e);
        }
    }
}
