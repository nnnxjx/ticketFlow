package com.ticketflow.service.impl;

import com.ticketflow.common.AppException;
import com.ticketflow.common.Ids;
import com.ticketflow.dto.auth.RegisterRequest;
import com.ticketflow.entity.auth.UserEntity;
import com.ticketflow.mapper.auth.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthServiceImplTest {
    private final UserMapper users = mock(UserMapper.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final JwtEncoder jwtEncoder = mock(JwtEncoder.class);
    private final Ids ids = mock(Ids.class);
    private AuthServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AuthServiceImpl();
        ReflectionTestUtils.setField(service, "baseMapper", users);
        ReflectionTestUtils.setField(service, "encoder", encoder);
        ReflectionTestUtils.setField(service, "jwtEncoder", jwtEncoder);
        ReflectionTestUtils.setField(service, "ids", ids);
        ReflectionTestUtils.setField(service, "ttlMinutes", 120L);
        when(ids.next()).thenReturn(1001L);
    }

    @Test
    void registerStoresHashInsteadOfPlainPassword() {
        when(encoder.encode("strongPassword123")).thenReturn("bcrypt-hash");
        when(users.insert((UserEntity) any(UserEntity.class))).thenReturn(1);
        var user = service.register(new RegisterRequest("13800138000", "strongPassword123"));
        assertEquals(1001L, user.getId());
        verify(users).insert(argThat((UserEntity entity) -> "bcrypt-hash".equals(entity.getPasswordHash())
                && !"strongPassword123".equals(entity.getPasswordHash())));
    }

    @Test
    void duplicatePhoneReturnsConflict() {
        when(encoder.encode(any())).thenReturn("bcrypt-hash");
        when(users.insert((UserEntity) any(UserEntity.class))).thenThrow(new DuplicateKeyException("duplicate"));
        AppException ex = assertThrows(AppException.class,
                () -> service.register(new RegisterRequest("13800138000", "strongPassword123")));
        assertEquals("PHONE_EXISTS", ex.code());
    }
}
