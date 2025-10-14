package com.globalsearch.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Slf4j
public class RateLimitInterceptor implements HandlerInterceptor {

    // Simple in-memory rate limiting (for production, use Redis or similar)
    private final Map<String, RateLimitInfo> requestCounts = new ConcurrentHashMap<>();

    // Rate limit: 100 requests per minute per IP
    private static final int MAX_REQUESTS_PER_MINUTE = 100;
    private static final long TIME_WINDOW_MS = 60_000; // 1 minute

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String clientIp = getClientIp(request);
        String endpoint = request.getRequestURI();

        log.debug("Rate limit check for IP: {} on endpoint: {}", clientIp, endpoint);

        RateLimitInfo limitInfo = requestCounts.computeIfAbsent(clientIp, k -> new RateLimitInfo());

        long currentTime = System.currentTimeMillis();

        // Reset counter if time window has passed
        if (currentTime - limitInfo.getWindowStart() > TIME_WINDOW_MS) {
            limitInfo.reset(currentTime);
        }

        int currentCount = limitInfo.incrementAndGet();

        // Add rate limit headers
        response.setHeader("X-RateLimit-Limit", String.valueOf(MAX_REQUESTS_PER_MINUTE));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, MAX_REQUESTS_PER_MINUTE - currentCount)));
        response.setHeader("X-RateLimit-Reset", String.valueOf(limitInfo.getWindowStart() + TIME_WINDOW_MS));

        if (currentCount > MAX_REQUESTS_PER_MINUTE) {
            log.warn("Rate limit exceeded for IP: {} ({}  requests)", clientIp, currentCount);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Rate limit exceeded\",\"message\":\"Too many requests. Please try again later.\"}");
            return false;
        }

        return true;
    }

    /**
     * Get client IP address from request
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    /**
     * Rate limit tracking info
     */
    private static class RateLimitInfo {
        private final AtomicInteger count;
        private long windowStart;

        public RateLimitInfo() {
            this.count = new AtomicInteger(0);
            this.windowStart = System.currentTimeMillis();
        }

        public int incrementAndGet() {
            return count.incrementAndGet();
        }

        public void reset(long newWindowStart) {
            count.set(0);
            windowStart = newWindowStart;
        }

        public long getWindowStart() {
            return windowStart;
        }
    }

    /**
     * Cleanup old entries periodically
     * In production, this should be scheduled
     */
    public void cleanup() {
        long currentTime = System.currentTimeMillis();
        requestCounts.entrySet().removeIf(entry ->
            currentTime - entry.getValue().getWindowStart() > TIME_WINDOW_MS * 2);
        log.debug("Cleaned up rate limit cache. Remaining entries: {}", requestCounts.size());
    }
}
