package org.nowstart.nyangnyangbot.application.port.in.favorite;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public interface ManageFavoriteAdjustmentUseCase {

    List<FavoriteAdjustmentOptionResult> getAdjustments();

    FavoriteAdjustmentOptionResult createAdjustment(
            @Valid @NotNull(message = "command is required") FavoriteAdjustmentCreateCommand command
    );

    FavoriteAdjustmentApplyResult applyAdjustments(
            @Valid @NotNull(message = "command is required") FavoriteAdjustmentApplyCommand command
    );

    record FavoriteAdjustmentCreateCommand(
            @NotNull(message = "amount is required")
            Integer amount,
            @NotBlank(message = "label is required")
            @Size(max = 255, message = "label length must be 255 or less")
            String label
    ) {
    }

    record FavoriteAdjustmentApplyCommand(
            @NotBlank(message = "userId is required")
            String userId,
            List<@Positive(message = "adjustmentIds must be positive") Long> adjustmentIds,
            Integer manualAmount,
            @Size(max = 255, message = "manualHistory length must be 255 or less")
            String manualHistory
    ) {

        @AssertTrue(message = "adjustmentIds or manualAmount is required")
        public boolean hasAdjustment() {
            return (adjustmentIds != null && !adjustmentIds.isEmpty())
                    || (manualAmount != null && manualAmount != 0);
        }
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
