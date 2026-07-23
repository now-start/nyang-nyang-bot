package org.nowstart.nyangnyangbot.application.port.out.google;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public interface GoogleSheetPort {

    List<GoogleSheetRow> readPointRows();

    record GoogleSheetRow(
            @NotBlank(message = "displayName is required") String displayName,
            @NotBlank(message = "userId is required") String userId,
            @NotNull(message = "point is required") Long point
    ) {
    }
}
