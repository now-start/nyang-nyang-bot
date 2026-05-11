package org.nowstart.nyangnyangbot.adapter.in.web.roulette;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.BDDMockito.given;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.adapter.in.web.roulette.response.RouletteRoundResponse;
import org.nowstart.nyangnyangbot.application.service.roulette.RouletteService;
import org.nowstart.nyangnyangbot.domain.model.RouletteRound;
import org.nowstart.nyangnyangbot.domain.type.ConversionMode;
import org.nowstart.nyangnyangbot.domain.type.RewardType;
import org.nowstart.nyangnyangbot.domain.type.RouletteRoundStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class FavoriteRouletteControllerTest {

    @Mock
    private RouletteService rouletteService;

    @Test
    void getMyResults_ShouldUseAuthenticatedUserId() {
        FavoriteRouletteController controller = new FavoriteRouletteController(rouletteService);
        RouletteRound round = new RouletteRound(
                1L,
                1L,
                "donation-1",
                "user-1",
                "닉네임",
                1,
                "호감도 +10",
                1_000,
                false,
                RewardType.FAVORITE,
                ConversionMode.AUTO,
                10,
                RouletteRoundStatus.APPLIED,
                99L,
                100L,
                null,
                0
        );
        given(rouletteService.getRecentRounds("user-1", 5)).willReturn(List.of(round));

        ResponseEntity<List<RouletteRoundResponse>> result = controller.getMyResults(
                new UsernamePasswordAuthenticationToken("user-1", "N/A"),
                5
        );

        then(result.getBody()).isEqualTo(List.of(RouletteRoundResponse.from(round)));
        BDDMockito.then(rouletteService).should().getRecentRounds("user-1", 5);
    }
}
