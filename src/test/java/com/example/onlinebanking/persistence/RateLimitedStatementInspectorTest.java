package com.example.onlinebanking.persistence;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RateLimitedStatementInspectorTest {
    private static final Duration DEFAULT_MAX_WAIT = Duration.ofSeconds(5);

    @AfterEach
    void disableLimiter() {
        RateLimitedStatementInspector.configure(false, 2, DEFAULT_MAX_WAIT);
    }

    @Test
    void rejectsWorkThatCannotObtainAPermitWithinTheConfiguredWait() {
        RateLimitedStatementInspector.configure(true, 1, Duration.ofMillis(1));
        new RateLimitedStatementInspector().inspect("select 1");

        assertThatThrownBy(() -> new RateLimitedStatementInspector().inspect("select 1"))
                .isInstanceOf(DatabaseBusyException.class);
    }

    @Test
    void spacesStatementStartsAtTheConfiguredRate() {
        RateLimitedStatementInspector.configure(true, 4, Duration.ofSeconds(1));
        List<Long> starts = new ArrayList<>();
        RateLimitedStatementInspector inspector = new RateLimitedStatementInspector();

        for (int index = 0; index < 3; index++) {
            inspector.inspect("select 1");
            starts.add(System.nanoTime());
        }

        long firstGapMillis = TimeUnit.NANOSECONDS.toMillis(starts.get(1) - starts.get(0));
        long secondGapMillis = TimeUnit.NANOSECONDS.toMillis(starts.get(2) - starts.get(1));
        assertThat(firstGapMillis).isGreaterThanOrEqualTo(200);
        assertThat(secondGapMillis).isGreaterThanOrEqualTo(200);
    }

    @Test
    void reconfigurationClearsPreviouslyReservedPermits() {
        RateLimitedStatementInspector inspector = new RateLimitedStatementInspector();
        RateLimitedStatementInspector.configure(true, 1, Duration.ofSeconds(2));
        inspector.inspect("select 1");

        RateLimitedStatementInspector.configure(true, 1, Duration.ofSeconds(2));
        long started = System.nanoTime();
        inspector.inspect("select 1");

        assertThat(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started)).isLessThan(200);
    }

    @Test
    void interruptionRestoresTheFlagAndRejectsTheWaitingStatement() throws Exception {
        RateLimitedStatementInspector inspector = new RateLimitedStatementInspector();
        RateLimitedStatementInspector.configure(true, 1, Duration.ofSeconds(2));
        inspector.inspect("select 1");

        CountDownLatch started = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        AtomicBoolean interrupted = new AtomicBoolean();
        Thread waitingThread = Thread.ofPlatform().start(() -> {
            started.countDown();
            try {
                inspector.inspect("select 1");
            } catch (Throwable exception) {
                failure.set(exception);
                interrupted.set(Thread.currentThread().isInterrupted());
            }
        });

        assertThat(started.await(1, TimeUnit.SECONDS)).isTrue();
        waitingThread.interrupt();
        waitingThread.join(1_000);

        assertThat(waitingThread.isAlive()).isFalse();
        assertThat(failure.get()).isInstanceOf(DatabaseBusyException.class);
        assertThat(interrupted).isTrue();
    }

    @Test
    void coordinatesConcurrentStatementStarts() throws Exception {
        RateLimitedStatementInspector.configure(true, 100, Duration.ofSeconds(1));
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch done = new CountDownLatch(2);

        pool.submit(() -> {
            new RateLimitedStatementInspector().inspect("select 1");
            done.countDown();
        });
        pool.submit(() -> {
            new RateLimitedStatementInspector().inspect("select 1");
            done.countDown();
        });

        assertThat(done.await(1, TimeUnit.SECONDS)).isTrue();

        pool.shutdownNow();
    }
}
