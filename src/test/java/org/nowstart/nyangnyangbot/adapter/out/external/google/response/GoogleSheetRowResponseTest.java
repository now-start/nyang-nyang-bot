package org.nowstart.nyangnyangbot.adapter.out.external.google.response;

import static org.assertj.core.api.BDDAssertions.then;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.nowstart.nyangnyangbot.application.port.out.google.GoogleSheetPort.GoogleSheetRow;

class GoogleSheetRowResponseTest {

    @Test
    @DisplayName("포인트 값이 숫자인 경우 정상적으로 DTO를 반환한다")
    void fromRow_ShouldReturnDto_WhenPointIsNumeric() {
        // 실행
        var dto = new GoogleSheetRowResponse(List.of("닉네임", "user-1", 12)).toGoogleSheetRow();

        // 검증
        then(dto).contains(new GoogleSheetRow("닉네임", "user-1", 12L));
    }

    @Test
    @DisplayName("포인트 값이 공백인 경우 빈 결과를 반환한다")
    void fromRow_ShouldReturnEmpty_WhenPointIsBlank() {
        // 실행
        var dto = new GoogleSheetRowResponse(List.of("닉네임", "user-1", " ")).toGoogleSheetRow();

        // 검증
        then(dto).isEmpty();
    }

    @Test
    @DisplayName("포인트 값이 숫자가 아닌 경우 빈 결과를 반환한다")
    void fromRow_ShouldReturnEmpty_WhenPointIsNotNumeric() {
        // 실행
        var dto = new GoogleSheetRowResponse(List.of("닉네임", "user-1", "abc")).toGoogleSheetRow();

        // 검증
        then(dto).isEmpty();
    }
}
