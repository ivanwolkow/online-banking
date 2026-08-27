package com.example.onlinebanking.persistence;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;

class RateLimitedStatementInspectorTest {
    @Test
    void rejectsWorkThatCannotObtainAPermitWithinTheConfiguredWait() {
        RateLimitedStatementInspector.configure(true, 1, Duration.ofMillis(1));
        new RateLimitedStatementInspector().inspect("select 1");
        assertThatThrownBy(() -> new RateLimitedStatementInspector().inspect("select 1")).isInstanceOf(DatabaseBusyException.class);
        RateLimitedStatementInspector.configure(false, 2, Duration.ofSeconds(5));
    }

    @Test
    void coordinatesConcurrentStatementStarts() throws Exception {
        RateLimitedStatementInspector.configure(true, 100, Duration.ofSeconds(1));
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch done = new CountDownLatch(2);
        pool.submit(() -> { new RateLimitedStatementInspector().inspect("select 1"); done.countDown(); });
        pool.submit(() -> { new RateLimitedStatementInspector().inspect("select 1"); done.countDown(); });
        assertThat(done.await(1, TimeUnit.SECONDS)).isTrue();
        pool.shutdownNow();
        RateLimitedStatementInspector.configure(false, 2, Duration.ofSeconds(5));
    }
}
