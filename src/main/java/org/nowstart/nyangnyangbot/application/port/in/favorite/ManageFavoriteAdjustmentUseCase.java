package org.nowstart.nyangnyangbot.application.port.in.favorite;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

public interface ManageFavoriteAdjustmentUseCase {

    List<FavoriteAdjustmentOptionResult> getAdjustments();

    FavoriteAdjustmentOptionResult createAdjustment(FavoriteAdjustmentCreateCommand command);

    FavoriteAdjustmentApplyResult applyAdjustments(FavoriteAdjustmentApplyCommand command);

    record FavoriteAdjustmentCreateCommand(
            @NotNull(message = "amount is required")
            Integer amount,
            @NotBlank(message = "label is required")
            String label
    ) {
    }

    record FavoriteAdjustmentApplyCommand(
            @NotBlank(message = "userId is required")
            String userId,
            List<@Positive(message = "adjustmentIds must be positive") Long> adjustmentIds,
            Integer manualAmount,
            String manualHistory
    ) {
    }

    record FavoriteAdjustmentApplyResult(
            String userId,
            Integer beforeFavorite,
            Integer delta,
            Integer afterFavorite,
            String history
    ) {
    }

    record FavoriteAdjustmentOptionResult(
            Long id,
            Integer amount,
            String label
    ) {
    }
}
