package com.ticketflow.config;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Base64;
import static org.junit.jupiter.api.Assertions.*;

class TicketCryptoTest {
    @Test
    void tokenCanBeRecoveredOnlyWithTheMatchingVersionedKey() {
        TicketCrypto crypto = new TicketCrypto();
        ReflectionTestUtils.setField(crypto, "encodedKey", "");
        ReflectionTestUtils.setField(crypto, "encodedKeyRing", "1:" + Base64.getEncoder().encodeToString(new byte[32]));
        ReflectionTestUtils.setField(crypto, "activeVersion", (short) 1);
        crypto.init();

        String token = crypto.newToken();
        assertEquals(32, crypto.hash(token).length);
        assertEquals(token, crypto.decrypt(crypto.encrypt(token), (short) 1));
        assertThrows(IllegalStateException.class, () -> crypto.decrypt(crypto.encrypt(token), (short) 2));
    }
}
