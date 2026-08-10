package org.nowstart.nyangnyangbot.adapter.out.transaction;

import java.util.function.Consumer;
import org.nowstart.nyangnyangbot.application.port.out.transaction.TransactionContextPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class SpringTransactionContextAdapter implements TransactionContextPort {

    @Override
    public boolean isTransactionActive() {
        return TransactionSynchronizationManager.isActualTransactionActive();
    }

    @Override
    public boolean registerAfterCompletion(Consumer<Boolean> completion) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return false;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                completion.accept(status == STATUS_COMMITTED);
            }
        });
        return true;
    }
}
