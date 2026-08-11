package org.nowstart.nyangnyangbot.application.port.out.point;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.util.List;

public interface PointAdjustmentPresetPort {

    List<PresetRecord> findAll();

    PresetRecord save(SavePresetCommand command);

    record SavePresetCommand(
            long amount,
            @NotBlank(message = "label is required") String label
    ) {
        @AssertTrue(message = "amount must not be zero")
        public boolean isAmountNonZero() {
            return amount != 0;
        }
    }

    record PresetRecord(
            @Positive(message = "id must be positive") long id,
            long amount,
            @NotBlank(message = "label is required") String label
    ) {
        @AssertTrue(message = "amount must not be zero")
        public boolean isAmountNonZero() {
            return amount != 0;
        }
    }
}
