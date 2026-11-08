package com.ticketflow.config;

import com.ticketflow.entity.auth.UserEntity;
import com.ticketflow.mapper.auth.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class JwtSignatureTest {
    @Test
    void modifiedJwtPayloadFailsSignatureVerification() {
        SecurityConfig config = new SecurityConfig();
        SecretKey key = config.jwtKey("ticketflow-signature-test-key-1234567890");
        UserMapper users = mock(UserMapper.class);
        UserEntity active = new UserEntity();
        active.setStatus("ACTIVE");
        when(users.selectById(anyLong())).thenReturn(active);
        JwtEncoder encoder = config.jwtEncoder(key);
        JwtDecoder decoder = config.jwtDecoder(key, users);
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer("ticketflow").subject("123")
                .issuedAt(now).expiresAt(now.plusSeconds(600)).build();
        String token = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        assertEquals("123", decoder.decode(token).getSubject());

        String[] parts = token.split("\\.");
        // 只改 payload、不重签名，模拟客户端篡改用户身份。
        String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        String changedPayload = payload.replace("\"sub\":\"123\"", "\"sub\":\"124\"");
        assertNotEquals(payload, changedPayload);
        String tampered = parts[0] + "." + Base64.getUrlEncoder().withoutPadding()
                .encodeToString(changedPayload.getBytes(StandardCharsets.UTF_8)) + "." + parts[2];
        assertThrows(JwtException.class, () -> decoder.decode(tampered));
    }
}
