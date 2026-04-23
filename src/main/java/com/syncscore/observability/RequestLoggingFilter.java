package com.syncscore.observability;

import com.syncscore.security.AccessPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path != null && path.startsWith("/actuator/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String correlationId = UUID.randomUUID().toString();
        response.setHeader("X-Correlation-Id", correlationId);

        long startNs = System.nanoTime();
        StatusCaptureResponseWrapper wrapped = new StatusCaptureResponseWrapper(response);
        try {
            filterChain.doFilter(request, wrapped);
        } finally {
            long durationMs = (System.nanoTime() - startNs) / 1_000_000L;
            int status = wrapped.getStatus();
            String method = request.getMethod();
            String path = request.getRequestURI();
            String userId = resolveUserId();

            if (status >= 500) {
                log.error("event=HTTP_REQUEST method={} path={} status={} durationMs={} correlationId={} userId={}",
                        method, path, status, durationMs, correlationId, userId);
            } else if (status >= 400) {
                log.warn("event=HTTP_REQUEST method={} path={} status={} durationMs={} correlationId={} userId={}",
                        method, path, status, durationMs, correlationId, userId);
            } else {
                log.info("event=HTTP_REQUEST method={} path={} status={} durationMs={} correlationId={} userId={}",
                        method, path, status, durationMs, correlationId, userId);
            }
        }
    }

    private String resolveUserId() {
        Authentication auth = SecurityContextHolder.getContext() != null
                ? SecurityContextHolder.getContext().getAuthentication()
                : null;
        if (auth == null) return "anonymous";
        Object principal = auth.getPrincipal();
        if (principal instanceof AccessPrincipal ap && ap.userId() != null) {
            return ap.userId().toString();
        }
        return "anonymous";
    }

    private static class StatusCaptureResponseWrapper extends HttpServletResponseWrapper {
        private int status = HttpServletResponse.SC_OK;

        StatusCaptureResponseWrapper(HttpServletResponse response) {
            super(response);
        }

        @Override
        public void setStatus(int sc) {
            this.status = sc;
            super.setStatus(sc);
        }

        @Override
        public void sendError(int sc) throws IOException {
            this.status = sc;
            super.sendError(sc);
        }

        @Override
        public void sendError(int sc, String msg) throws IOException {
            this.status = sc;
            super.sendError(sc, msg);
        }

        @Override
        public void sendRedirect(String location) throws IOException {
            this.status = HttpServletResponse.SC_FOUND;
            super.sendRedirect(location);
        }

        @Override
        public int getStatus() {
            return status;
        }
    }
}

