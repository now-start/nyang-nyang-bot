package org.nowstart.nyangnyangbot.application.port.in.presence;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

public interface ManagePresenceRewardUseCase {

    /** 이전 수집 상태를 초기화하고 새로운 출석 수집 세션을 시작한다. */
    void startCapture();

    /** 현재 출석 수집 세션을 종료한다. */
    void stopCapture();

    /** 현재 수집 세션에서 확인된 사용자를 반환한다. */
    List<PresenceUserSnapshot> getActiveUsers();

    /** 현재 출석 목록에서 선택한 사용자에게 요청된 보상을 지급한다. */
    void applyPresenceReward(
            @Valid @NotNull(message = "command is required") PresenceApplyCommand command
    );

    record PresenceApplyCommand(
            @NotEmpty(message = "presence targets are required")
            List<@NotBlank(message = "userId is required") String> userIds,
            @NotNull(message = "amount is required")
            @Positive(message = "amount must be positive") Long amount
    ) {
    }

    record PresenceUserSnapshot(String userId, String displayName, long lastMessageTime) {
    }
}
