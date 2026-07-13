package org.nowstart.nyangnyangbot.application.port.out.favorite;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.nowstart.nyangnyangbot.application.validation.outbound.OutboundResult;

public interface FavoriteAdjustmentPort {

    List<OptionResult> findAll();

    OptionResult save(Integer amount, String label);

    record OptionResult(
            @NotNull(groups = OutboundResult.class, message = "id is required")
            @Positive(groups = OutboundResult.class, message = "id must be positive")
            Long id,
            @NotNull(message = "amount is required")
            Integer amount,
            @NotBlank(message = "label is required")
            String label
    ) {
    }
}
