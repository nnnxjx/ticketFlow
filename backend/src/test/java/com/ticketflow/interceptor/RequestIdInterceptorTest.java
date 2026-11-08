package com.ticketflow.interceptor;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

class RequestIdInterceptorTest {
    @Test
    void preservesSafeRequestIdInResponse() {
        RequestIdInterceptor interceptor = new RequestIdInterceptor();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("X-Request-Id", "purchase_123");

        assertTrue(interceptor.preHandle(request, response, new Object()));
        assertEquals("purchase_123", request.getAttribute(RequestIdInterceptor.ATTRIBUTE));
        assertEquals("purchase_123", response.getHeader("X-Request-Id"));
        interceptor.afterCompletion(request, response, new Object(), null);
    }
}
