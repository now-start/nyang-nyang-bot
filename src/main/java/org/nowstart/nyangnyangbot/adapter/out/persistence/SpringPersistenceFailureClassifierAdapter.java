package org.nowstart.nyangnyangbot.adapter.out.persistence;

import org.nowstart.nyangnyangbot.application.port.out.persistence.PersistenceFailureClassifierPort;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.stereotype.Component;

@Component
public class SpringPersistenceFailureClassifierAdapter implements PersistenceFailureClassifierPort {

    @Override
    public boolean isConflict(RuntimeException failure) {
        return contains(failure, DataIntegrityViolationException.class);
    }

    @Override
    public boolean isRetryable(RuntimeException failure) {
        return contains(failure, TransientDataAccessException.class)
                || contains(failure, RecoverableDataAccessException.class)
                || contains(failure, DataAccessResourceFailureException.class);
    }

    private boolean contains(Throwable failure, Class<? extends Throwable> failureType) {
        Throwable cause = failure;
        while (cause != null) {
            if (failureType.isInstance(cause)) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }
}
