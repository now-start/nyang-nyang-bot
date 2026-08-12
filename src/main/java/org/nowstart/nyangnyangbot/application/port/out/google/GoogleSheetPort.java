package org.nowstart.nyangnyangbot.application.port.out.google;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public interface GoogleSheetPort {

    /** 설정된 Google Sheet에서 포인트 잔액 행을 읽는다. */
    List<@Valid GoogleSheetRow> readPointRows();

    record GoogleSheetRow(
            @NotBlank(message = "displayName is required") String displayName,
            @NotBlank(message = "userId is required") String userId,
            @NotNull(message = "point is required") Long point
    ) {
    }
}
