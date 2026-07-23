package org.nowstart.nyangnyangbot.application.port.out.user;

import java.time.Instant;
import java.util.Optional;

public interface UserAccountPort {

    void observe(String userId, String displayName);

    Optional<UserAccountRecord> findById(String userId);

    record UserAccountRecord(
            String userId,
            String displayName,
            boolean admin,
            Instant lastLoginAt
    ) {
    }
}
