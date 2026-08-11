package org.nowstart.nyangnyangbot.application.port.out.user;

import jakarta.validation.constraints.NotBlank;
import java.util.Optional;

public interface UserAccountPort {

    void observe(ObserveUserCommand command);

    Optional<String> findDisplayNameById(String userId);

    record ObserveUserCommand(
            @NotBlank(message = "userId is required") String userId,
            String displayName
    ) {
    }
}
