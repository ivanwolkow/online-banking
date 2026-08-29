package com.example.onlinebanking.persistence;

import com.google.common.util.concurrent.RateLimiter;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DatabaseOperationGateTest {
    @Test
    void rejectsAnOperationWhenGuavaHasNoImmediatePermit() {
        DatabaseOperationGate gate = new DatabaseOperationGate(true, RateLimiter.create(2));

        gate.acquirePermit();

        assertThatThrownBy(gate::acquirePermit)
                .isInstanceOf(DatabaseBusyException.class);
    }

    @Test
    void disabledGateDoesNotConsumeGuavaPermits() {
        DatabaseOperationGate gate = new DatabaseOperationGate(false, RateLimiter.create(1));

        gate.acquirePermit();

        assertThatCode(gate::acquirePermit).doesNotThrowAnyException();
    }
}
