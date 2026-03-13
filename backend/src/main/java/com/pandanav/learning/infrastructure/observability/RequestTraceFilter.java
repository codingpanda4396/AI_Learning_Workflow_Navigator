package com.pandanav.learning.infrastructure.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestTraceFilter extends OncePerRequestFilter {

    private static final String TRACE_HEADER = "X-Trace-Id";
    private static final String REQUEST_HEADER = "X-Request-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        try {
            TraceContext.put(request.getHeader(TRACE_HEADER), request.getHeader(REQUEST_HEADER));
            response.setHeader(TRACE_HEADER, TraceContext.traceId());
            response.setHeader(REQUEST_HEADER, TraceContext.requestId());
            filterChain.doFilter(request, response);
        } finally {
            TraceContext.clear();
        }
    }
}
