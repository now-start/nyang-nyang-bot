package org.nowstart.nyangnyangbot.adapter.out.persistence;

import static org.assertj.core.api.BDDAssertions.then;

import org.junit.jupiter.api.Test;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;

class SpringPersistenceFailureClassifierAdapterTest {

    private final SpringPersistenceFailureClassifierAdapter adapter =
            new SpringPersistenceFailureClassifierAdapter();

    @Test
    void classifiesConflictAndRetryableFailuresThroughCauseChain() {
        RuntimeException conflict = new IllegalStateException(
                new DataIntegrityViolationException("duplicate")
        );
        RuntimeException retryable = new IllegalStateException(
                new CannotAcquireLockException("locked")
        );

        then(adapter.isConflict(conflict)).isTrue();
        then(adapter.isRetryable(retryable)).isTrue();
        then(adapter.isConflict(new IllegalStateException("other"))).isFalse();
        then(adapter.isRetryable(new IllegalStateException("other"))).isFalse();
    }
}
