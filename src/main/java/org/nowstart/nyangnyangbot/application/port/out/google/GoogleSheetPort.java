package org.nowstart.nyangnyangbot.application.port.out.google;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public interface GoogleSheetPort {

    List<GoogleSheetRow> readFavoriteRows();

    record GoogleSheetRow(
            @NotBlank(message = "nickName is required") String nickName,
            @NotBlank(message = "userId is required") String userId,
            @NotNull(message = "favorite is required") Integer favorite
    ) {
    }
}
