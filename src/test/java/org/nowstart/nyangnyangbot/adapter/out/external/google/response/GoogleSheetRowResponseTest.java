package org.nowstart.nyangnyangbot.adapter.out.external.google.response;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.nowstart.nyangnyangbot.application.port.out.google.GoogleSheetPort.GoogleSheetRow;

class GoogleSheetRowResponseTest {

    @Test
    void fromRow_ShouldReturnDto_WhenFavoriteIsNumeric() {
        GoogleSheetRow dto = new GoogleSheetRowResponse(List.of("닉네임", "user-1", 12)).toGoogleSheetRow();

        assertThat(dto).isEqualTo(new GoogleSheetRow("닉네임", "user-1", 12));
    }

    @Test
    void fromRow_ShouldReturnNull_WhenFavoriteIsBlank() {
        GoogleSheetRow dto = new GoogleSheetRowResponse(List.of("닉네임", "user-1", " ")).toGoogleSheetRow();

        assertThat(dto).isNull();
    }

    @Test
    void fromRow_ShouldReturnNull_WhenFavoriteIsNotNumeric() {
        GoogleSheetRow dto = new GoogleSheetRowResponse(List.of("닉네임", "user-1", "abc")).toGoogleSheetRow();

        assertThat(dto).isNull();
    }
}
