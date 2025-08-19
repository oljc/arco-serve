package io.github.oljc.arcoserve.shared.web;

import org.slf4j.MDC;
import org.slf4j.MDC.MDCCloseable;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String traceId = getTraceId();

        HttpServletRequest wrapped = request;
        if (isCache(request) && !(request instanceof CachedRequest)) {
            wrapped = new CachedRequest(request);
        }

        try (MDCCloseable ignored = MDC.putCloseable("traceId", traceId)) {
            filterChain.doFilter(wrapped, response);
        }
    }

    private String getTraceId() {
        String traceId = MDC.get("traceId");
        return traceId != null ? traceId : java.util.UUID.randomUUID().toString().replace("-", "");
    }

    private boolean isCache(HttpServletRequest request) {
        final String method = request.getMethod();
        switch (method) {
            case "POST":
            case "PUT":
            case "PATCH":
            case "DELETE":
                break;
            default:
                return false;
        }

        long len = request.getContentLengthLong();
        if (len == 0L) return false;
        if (len > 0 && len > 1024 * 1024 * 2) return false;

        String ct = request.getContentType();
        if(ct != null) {
            if (startsWithIgnoreCase(ct, "multipart/")) return false;
		    if (startsWithIgnoreCase(ct, "application/octet-stream")) return false;
        }

        final String upgrade = request.getHeader("Upgrade");
        if (upgrade != null && upgrade.equalsIgnoreCase("websocket")) return false;

        return true;
    }

    private static boolean startsWithIgnoreCase(String str, String prefix) {
        final int len = prefix.length();
        return str.length() >= len && str.regionMatches(true, 0, prefix, 0, len);
    }
}
