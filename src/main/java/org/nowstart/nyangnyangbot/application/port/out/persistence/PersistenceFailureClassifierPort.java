package org.nowstart.nyangnyangbot.application.port.out.persistence;

public interface PersistenceFailureClassifierPort {

    boolean isConflict(RuntimeException failure);

    boolean isRetryable(RuntimeException failure);
}
