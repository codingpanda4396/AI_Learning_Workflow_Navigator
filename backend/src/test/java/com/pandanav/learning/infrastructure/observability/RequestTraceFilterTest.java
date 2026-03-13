package com.pandanav.learning.infrastructure.observability;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RequestTraceFilterTest {

    @Test
    void shouldPopulateTraceHeaders() throws ServletException, IOException {
        RequestTraceFilter filter = new RequestTraceFilter();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertNotNull(response.getHeader("X-Trace-Id"));
        assertNotNull(response.getHeader("X-Request-Id"));
        assertFalse(response.getHeader("X-Trace-Id").isBlank());
        assertFalse(response.getHeader("X-Request-Id").isBlank());
    }
}
