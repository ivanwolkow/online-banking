package com.example.onlinebanking.persistence;

import com.example.onlinebanking.config.AppProperties;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

@Component
class RateLimiterConfiguration {
    private final AppProperties properties;

    RateLimiterConfiguration(AppProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void configure() {
        AppProperties.RateLimit limit = properties.database().rateLimit();
        RateLimitedStatementInspector.configure(limit.enabled(), limit.operationsPerSecond(), limit.maxWait());
    }
}
