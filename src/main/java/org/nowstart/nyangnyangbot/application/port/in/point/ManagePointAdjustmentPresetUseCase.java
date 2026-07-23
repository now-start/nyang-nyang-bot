package org.nowstart.nyangnyangbot.application.port.in.point;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public interface ManagePointAdjustmentPresetUseCase {

    List<PointAdjustmentPresetResult> getPresets();

    PointAdjustmentPresetResult createPreset(
            @Valid @NotNull(message = "command is required") CreatePointAdjustmentPreset command
    );

    void applyAdjustments(
            @Valid @NotNull(message = "command is required") ApplyPointAdjustments command
    );

    record CreatePointAdjustmentPreset(
            @NotNull(message = "amount is required") Long amount,
            @NotBlank(message = "label is required")
            @Size(max = 100, message = "label length must be 100 or less") String label
    ) {
        @AssertTrue(message = "amount must not be zero")
        public boolean isAmountNonZero() {
            return amount != null && amount != 0;
        }
    }

    record ApplyPointAdjustments(
            @NotBlank(message = "userId is required") String userId,
            List<@Positive(message = "presetIds must be positive") Long> presetIds,
            Long manualAmount,
            @Size(max = 500, message = "manualDescription length must be 500 or less") String manualDescription,
            String actorUserId
    ) {
        @AssertTrue(message = "presetIds or manualAmount is required")
        public boolean hasAdjustment() {
            return (presetIds != null && !presetIds.isEmpty())
                    || (manualAmount != null && manualAmount != 0);
        }
    }

    record PointAdjustmentPresetResult(long id, long amount, String label) {
    }
}
