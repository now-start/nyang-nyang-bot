package org.nowstart.nyangnyangbot.application.port.out.transaction;

import java.util.function.Consumer;

public interface TransactionContextPort {

    boolean isTransactionActive();

    boolean registerAfterCompletion(Consumer<Boolean> completion);
}
