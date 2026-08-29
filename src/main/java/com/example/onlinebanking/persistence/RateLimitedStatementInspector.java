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
        enabled = limiterEnabled;
        intervalNanos = Duration.ofSeconds(1).toNanos() / operationsPerSecond;
        maxWaitNanos = maximumWait.toNanos();
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

        long now = System.nanoTime();
        long scheduled;

        synchronized (MONITOR) {
            scheduled = Math.max(now, nextPermitNanos);
            if (scheduled - now > maxWaitNanos) {
                throw new DatabaseBusyException();
            }

            nextPermitNanos = scheduled + intervalNanos;
        }

        long remaining = scheduled - System.nanoTime();
        if (remaining > 0) {
            try {
                Thread.sleep(remaining / 1_000_000L, (int) (remaining % 1_000_000L));
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new DatabaseBusyException();
            }
        }
    }
}
