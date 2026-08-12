package org.nowstart.nyangnyangbot.application.port.out.point;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

public interface PointAdjustmentPresetPort {

    /** 모든 포인트 조정 프리셋을 반환한다. */
    @Valid
    List<PresetRecord> findAll();

    /** 포인트 조정 프리셋을 저장하고 저장된 결과를 반환한다. */
    @Valid
    PresetRecord save(@Valid @NotNull(message = "preset command is required") SavePresetCommand command);

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
