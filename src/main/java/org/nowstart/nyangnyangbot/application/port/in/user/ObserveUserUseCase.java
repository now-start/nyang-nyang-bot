package org.nowstart.nyangnyangbot.application.port.in.user;

import jakarta.validation.constraints.NotBlank;

public interface ObserveUserUseCase {

    /** 최근 확인한 사용자 정보로 계정을 생성하거나 갱신한다. */
    void observeUser(@NotBlank(message = "userId is required") String userId, String displayName);
}
