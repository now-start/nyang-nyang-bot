package org.nowstart.nyangnyangbot.adapter.out.transaction;

import static org.assertj.core.api.BDDAssertions.then;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class SpringTransactionContextAdapterTest {

    private final SpringTransactionContextAdapter adapter = new SpringTransactionContextAdapter();

    @AfterEach
    void clearTransactionContext() {
        TransactionSynchronizationManager.clear();
    }

    @Test
    void exposesActiveTransactionAndAfterCompletion() {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();
        AtomicReference<Boolean> committed = new AtomicReference<>();

        boolean registered = adapter.registerAfterCompletion(committed::set);
        TransactionSynchronizationManager.getSynchronizations().forEach(synchronization ->
                synchronization.afterCompletion(TransactionSynchronization.STATUS_COMMITTED)
        );

        then(adapter.isTransactionActive()).isTrue();
        then(registered).isTrue();
        then(committed).hasValue(true);
    }

    @Test
    void returnsFalseWithoutSynchronization() {
        then(adapter.registerAfterCompletion(committed -> { })).isFalse();
    }
}
