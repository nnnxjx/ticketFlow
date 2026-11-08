package com.ticketflow.interceptor;

import com.ticketflow.common.AppException;
import com.ticketflow.common.UserContext;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthInterceptorTest {
    @Test
    void missingTokenIsRejected() {
        AuthInterceptor interceptor = new AuthInterceptor();
        AppException ex = assertThrows(AppException.class,
                () -> interceptor.preHandle(new MockHttpServletRequest(), new MockHttpServletResponse(), new Object()));
        assertEquals("UNAUTHORIZED", ex.code());
    }

    @Test
    void validTokenMakesUserIdAvailableToController() {
        JwtDecoder decoder = mock(JwtDecoder.class);
        Jwt jwt = mock(Jwt.class);
        when(decoder.decode("test-token")).thenReturn(jwt);
        when(jwt.getSubject()).thenReturn("123");
        AuthInterceptor interceptor = new AuthInterceptor();
        ReflectionTestUtils.setField(interceptor, "jwtDecoder", decoder);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "test-token");

        assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));
        assertEquals(123L, UserContext.getUserId());
        interceptor.afterCompletion(request, new MockHttpServletResponse(), new Object(), null);
        assertThrows(AppException.class, UserContext::getUserId);
    }
}
