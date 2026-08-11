package org.nowstart.nyangnyangbot.application.port.in.user;

import jakarta.validation.constraints.NotBlank;

public interface ObserveUserUseCase {

    void observeUser(@NotBlank(message = "userId is required") String userId, String displayName);
}
