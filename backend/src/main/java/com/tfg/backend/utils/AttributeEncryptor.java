package com.tfg.backend.utils;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.util.Base64;

@Component
@Converter
public class AttributeEncryptor implements AttributeConverter<String, String> {

    private static final String ALGORITHM = "AES";

    private final Key key;

    public AttributeEncryptor(@Value("${app.cards-db.key}") String secret) {
        this.key = new SecretKeySpec(secret.getBytes(), ALGORITHM);
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        // Encrypt
        if (attribute == null) return null;
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return Base64.getEncoder().encodeToString(cipher.doFinal(attribute.getBytes()));
        } catch (Exception e) {
            throw new IllegalStateException("Error al cifrar datos de tarjeta:", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        // Decrypt
        if (dbData == null) return null;
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key);
            return new String(cipher.doFinal(Base64.getDecoder().decode(dbData)));
        } catch (Exception e) {
            throw new IllegalStateException("Error al descifrar datos de tarjeta: ", e);
        }
    }
}