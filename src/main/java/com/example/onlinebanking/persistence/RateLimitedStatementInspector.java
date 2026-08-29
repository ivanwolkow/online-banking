package com.example.onlinebanking.persistence;

import org.hibernate.resource.jdbc.spi.StatementInspector;

import java.time.Duration;

/** Hibernate creates this inspector itself; configuration is supplied at application startup. */
public class RateLimitedStatementInspector implements StatementInspector {
    private static final Object MONITOR = new Object();
    private static volatile boolean enabled = true;
    private static volatile long intervalNanos = Duration.ofMillis(500).toNanos();
    private static volatile long maxWaitNanos = Duration.ofSeconds(5).toNanos();
    private static long nextPermitNanos;

    public static void configure(boolean limiterEnabled, int operationsPerSecond, Duration maximumWait) {
        if (operationsPerSecond <= 0) {
            throw new IllegalArgumentException("Operations per second must be positive");
        }
        if (maximumWait == null || maximumWait.isNegative() || maximumWait.isZero()) {
            throw new IllegalArgumentException("Maximum wait must be positive");
        }

        synchronized (MONITOR) {
            enabled = false;
            intervalNanos = Duration.ofSeconds(1).toNanos() / operationsPerSecond;
            maxWaitNanos = maximumWait.toNanos();
            nextPermitNanos = 0;
            enabled = limiterEnabled;
        }
    }

    @Override
    public String inspect(String sql) {
        acquirePermit();
        return sql;
    }

    static void acquirePermit() {
        if (!enabled) {
            return;
        }

        long scheduled;

        synchronized (MONITOR) {
            long now = System.nanoTime();
            scheduled = Math.max(now, nextPermitNanos);
            if (scheduled - now > maxWaitNanos) {
                throw new DatabaseBusyException();
            }

            nextPermitNanos = scheduled + intervalNanos;
        }

        waitUntil(scheduled);
    }

    private static void waitUntil(long scheduled) {
        long remaining;
        while ((remaining = scheduled - System.nanoTime()) > 0) {
            try {
                Thread.sleep(remaining / 1_000_000L, (int) (remaining % 1_000_000L));
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new DatabaseBusyException();
            }
        }
    }
}
