package org.nowstart.nyangnyangbot.application.port.out.roulette;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

/** 메시지 템플릿 변수에서 사용하는 읽기 전용 최근 룰렛 조회 계약이다. */
public interface RecentRouletteResultQueryPort {

    int MAX_RECENT_ROUNDS = 5;

    /** 조회 사용자의 최근 룰렛 회차를 최대 {@value #MAX_RECENT_ROUNDS}개 반환한다. */
    @Valid
    List<RecentRound> findRecentRoundsByUserId(String userId);

    record RecentRound(
            @NotNull(message = "roundNo is required")
            @Positive(message = "roundNo must be positive") Integer roundNo,
            @NotBlank(message = "itemLabel is required") String itemLabel
    ) {
    }
}
