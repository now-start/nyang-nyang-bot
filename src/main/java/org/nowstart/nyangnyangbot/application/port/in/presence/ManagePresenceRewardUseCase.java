package org.nowstart.nyangnyangbot.application.port.in.presence;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

public interface ManagePresenceRewardUseCase {

    void startCapture();

    void stopCapture();

    List<PresenceUserSnapshot> getActiveUsers();

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
