package org.nowstart.nyangnyangbot.application.port.in.reward;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.nowstart.nyangnyangbot.application.port.in.reward.QueryRewardUseCase.RewardResult;

public interface GrantRewardUseCase {

    RewardResult grantManual(@Valid @NotNull GrantManualRewardCommand command, String actorUserId);

    record GrantManualRewardCommand(
            @NotBlank String userId,
            @NotBlank @Size(max = 100) String label,
            @NotBlank String rewardType,
            @NotBlank String conversionMode,
            Long pointDelta,
            @NotBlank @Size(max = 500) String description,
            @Size(max = 500) String privateNote,
            @NotBlank @Size(max = 177) String idempotencyKey
    ) {
    }
}
