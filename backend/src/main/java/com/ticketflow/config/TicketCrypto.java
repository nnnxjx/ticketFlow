package com.ticketflow.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Component
public class TicketCrypto {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int IV_BYTES = 12;
    @Value("${ticketflow.ticket.encryption-key:}") private String encodedKey;
    @Value("${ticketflow.ticket.key-ring:}") private String encodedKeyRing;
    @Value("${ticketflow.ticket.active-key-version:1}") private short activeVersion;
    private Map<Short, SecretKeySpec> keys;

    @PostConstruct
    void init() {
        Map<Short, SecretKeySpec> configured = new HashMap<>();
        if (encodedKeyRing != null && !encodedKeyRing.isBlank()) {
            for (String entry : encodedKeyRing.split(",")) {
                String[] pair = entry.trim().split(":", 2);
                if (pair.length != 2) throw new IllegalStateException("Invalid ticket key ring entry");
                configured.put(Short.parseShort(pair[0]), parseKey(pair[1]));
            }
        } else {
            configured.put((short) 1, parseKey(encodedKey));
        }
        if (!configured.containsKey(activeVersion)) throw new IllegalStateException("Active ticket key version is missing");
        keys = Map.copyOf(configured);
    }

    public short activeVersion() { return activeVersion; }

    private SecretKeySpec parseKey(String encoded) {
        byte[] bytes;
        try { bytes = Base64.getDecoder().decode(encoded); }
        catch (IllegalArgumentException ex) { throw new IllegalStateException("Ticket encryption key must be base64", ex); }
        if (bytes.length != 32) throw new IllegalStateException("Ticket encryption key must contain 32 bytes");
        return new SecretKeySpec(bytes, "AES");
    }

    public String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public byte[] hash(String token) {
        try { return MessageDigest.getInstance("SHA-256").digest(Base64.getUrlDecoder().decode(token)); }
        catch (Exception ex) { throw new IllegalArgumentException("Invalid ticket token", ex); }
    }

    public byte[] encrypt(String token) {
        try {
            byte[] iv = new byte[IV_BYTES];
            RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, keys.get(activeVersion), new GCMParameterSpec(128, iv));
            byte[] ciphertext = cipher.doFinal(token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return ByteBuffer.allocate(iv.length + ciphertext.length).put(iv).put(ciphertext).array();
        } catch (Exception ex) { throw new IllegalStateException("Ticket encryption failed", ex); }
    }

    public String decrypt(byte[] stored, short version) {
        try {
            SecretKeySpec key = keys.get(version);
            if (key == null) throw new IllegalStateException("Ticket key version is unavailable");
            if (stored == null || stored.length <= IV_BYTES) throw new IllegalArgumentException("Invalid ciphertext");
            ByteBuffer buffer = ByteBuffer.wrap(stored);
            byte[] iv = new byte[IV_BYTES];
            buffer.get(iv);
            byte[] ciphertext = new byte[buffer.remaining()];
            buffer.get(ciphertext);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));
            return new String(cipher.doFinal(ciphertext), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception ex) { throw new IllegalStateException("Ticket decryption failed", ex); }
    }
}
