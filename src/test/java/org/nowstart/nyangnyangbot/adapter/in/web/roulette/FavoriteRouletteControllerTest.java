package org.nowstart.nyangnyangbot.adapter.in.web.roulette;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.BDDMockito.given;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.adapter.in.web.roulette.response.RouletteRoundResponse;
import org.nowstart.nyangnyangbot.application.port.in.roulette.QueryRouletteResultUseCase;
import org.nowstart.nyangnyangbot.application.port.in.roulette.QueryRouletteResultUseCase.RouletteRoundResult;
import org.nowstart.nyangnyangbot.domain.type.ConversionMode;
import org.nowstart.nyangnyangbot.domain.type.RewardType;
import org.nowstart.nyangnyangbot.domain.type.RouletteRoundStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class FavoriteRouletteControllerTest {

    @Mock
    private QueryRouletteResultUseCase rouletteService;

    @Test
    @DisplayName("내 결과 조회 시 인증된 사용자 ID를 사용한다")
    void getMyResults_ShouldUseAuthenticatedUserId() {
        // 준비
        FavoriteRouletteController controller = new FavoriteRouletteController(rouletteService);
        RouletteRoundResult round = new RouletteRoundResult(
                1L,
                1,
                "호감도 +10",
                false,
                RewardType.FAVORITE.name(),
                ConversionMode.AUTO.name(),
                10,
                RouletteRoundStatus.APPLIED.name(),
                99L,
                100L,
                null
        );
        given(rouletteService.getRecentRounds("user-1", 5)).willReturn(List.of(round));

        // 실행
        ResponseEntity<List<RouletteRoundResponse>> result = controller.getMyResults(
                new UsernamePasswordAuthenticationToken("user-1", "N/A"),
                5
        );

        // 검증
        then(result.getBody()).isEqualTo(List.of(RouletteRoundResponse.from(round)));
        BDDMockito.then(rouletteService).should().getRecentRounds("user-1", 5);
    }
}
