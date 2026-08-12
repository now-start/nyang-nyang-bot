package org.nowstart.nyangnyangbot.application.port.out.transaction;

import java.util.function.Consumer;

public interface TransactionContextPort {

    /** 호출자가 현재 실제 트랜잭션 안에서 실행 중인지 반환한다. */
    boolean isTransactionActive();

    /**
     * 커밋 후에는 {@code true}, 그 외 종료 후에는 {@code false}를 전달받는 콜백을 등록한다.
     *
     * @return 트랜잭션 동기화가 활성화되어 콜백을 등록했으면 {@code true}
     */
    boolean registerAfterCompletion(Consumer<Boolean> completion);
}
