package com.example.onlinebanking.persistence;

import com.example.onlinebanking.config.AppProperties;
import com.google.common.util.concurrent.RateLimiter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Applies the application's database-capacity policy using Guava's in-memory rate limiter.
 */
@Component
public class DatabaseOperationGate {
    private final boolean enabled;
    private final RateLimiter rateLimiter;

    @Autowired
    public DatabaseOperationGate(AppProperties properties) {
        this(
                properties.database().rateLimit().enabled(),
                RateLimiter.create(properties.database().rateLimit().operationsPerSecond())
        );
    }

    DatabaseOperationGate(boolean enabled, RateLimiter rateLimiter) {
        this.enabled = enabled;
        this.rateLimiter = rateLimiter;
    }

    public void acquirePermit() {
        if (enabled && !rateLimiter.tryAcquire()) {
            throw new DatabaseBusyException();
        }
    }
}
