package com.ecommerce.main.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_REQUESTS_PER_MINUTE = 10;

    private final ConcurrentHashMap<String, RequestCount> requestCounts = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        if (path.equals("/api/auth/login") || path.equals("/api/auth/register") || path.equals("/api/auth/forgot-password")) {
            String ip = getClientIp(request);
            if (isRateLimited(ip)) {
                response.setStatus(429);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"Too many requests. Please try again later.\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isRateLimited(String ip) {
        long now = Instant.now().getEpochSecond();
        requestCounts.compute(ip, (key, val) -> {
            if (val == null || now - val.windowStart >= 60) {
                return new RequestCount(now, 1);
            }
            val.count++;
            return val;
        });

        RequestCount count = requestCounts.get(ip);
        return count != null && count.count > MAX_REQUESTS_PER_MINUTE;
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static class RequestCount {
        long windowStart;
        int count;

        RequestCount(long windowStart, int count) {
            this.windowStart = windowStart;
            this.count = count;
        }
    }
}
