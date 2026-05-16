package org.nowstart.nyangnyangbot.adapter.out.external.google.response;

import static org.assertj.core.api.BDDAssertions.then;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.nowstart.nyangnyangbot.application.port.out.google.GoogleSheetPort.GoogleSheetRow;

class GoogleSheetRowResponseTest {

    @Test
    void fromRow_ShouldReturnDto_WhenFavoriteIsNumeric() {
        // 실행
        GoogleSheetRow dto = new GoogleSheetRowResponse(List.of("닉네임", "user-1", 12)).toGoogleSheetRow();

        // 검증
        then(dto).isEqualTo(new GoogleSheetRow("닉네임", "user-1", 12));
    }

    @Test
    void fromRow_ShouldReturnNull_WhenFavoriteIsBlank() {
        // 실행
        GoogleSheetRow dto = new GoogleSheetRowResponse(List.of("닉네임", "user-1", " ")).toGoogleSheetRow();

        // 검증
        then(dto).isNull();
    }

    @Test
    void fromRow_ShouldReturnNull_WhenFavoriteIsNotNumeric() {
        // 실행
        GoogleSheetRow dto = new GoogleSheetRowResponse(List.of("닉네임", "user-1", "abc")).toGoogleSheetRow();

        // 검증
        then(dto).isNull();
    }
}
