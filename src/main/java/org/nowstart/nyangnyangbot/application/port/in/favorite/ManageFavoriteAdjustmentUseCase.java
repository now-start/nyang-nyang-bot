package org.nowstart.nyangnyangbot.application.port.in.favorite;

import java.util.List;

public interface ManageFavoriteAdjustmentUseCase {

    List<FavoriteAdjustmentOptionResult> getAdjustments();

    FavoriteAdjustmentOptionResult createAdjustment(FavoriteAdjustmentCreateCommand command);

    FavoriteAdjustmentApplyResult applyAdjustments(FavoriteAdjustmentApplyCommand command);

    record FavoriteAdjustmentCreateCommand(
            Integer amount,
            String label
    ) {
    }

    record FavoriteAdjustmentApplyCommand(
            String userId,
            List<Long> adjustmentIds,
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
