package org.nowstart.nyangnyangbot.application.port.out.user;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Optional;

public interface UserAccountPort {

    /** 사용자를 생성하거나 기존 사용자의 표시 이름을 갱신한다. */
    void observe(@Valid @NotNull(message = "observe user command is required") ObserveUserCommand command);

    /** 등록된 사용자의 현재 표시 이름을 반환한다. */
    Optional<String> findDisplayNameById(String userId);

    record ObserveUserCommand(
            @NotBlank(message = "userId is required") String userId,
            String displayName
    ) {
    }
}
