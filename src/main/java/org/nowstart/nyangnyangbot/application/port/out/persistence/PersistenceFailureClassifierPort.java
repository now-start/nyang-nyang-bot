package org.nowstart.nyangnyangbot.application.port.out.persistence;

public interface PersistenceFailureClassifierPort {

    /** 실패가 영속성 계층의 고유성 또는 동시성 충돌인지 반환한다. */
    boolean isConflict(RuntimeException failure);

    /** 실패한 영속성 작업을 재시도하면 성공할 가능성이 있는지 반환한다. */
    boolean isRetryable(RuntimeException failure);
}
