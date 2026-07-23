package org.nowstart.nyangnyangbot.application.port.out.user;

import java.util.Optional;

public interface UserAccountPort {

    void observe(String userId, String displayName);

    Optional<String> findDisplayNameById(String userId);
}
