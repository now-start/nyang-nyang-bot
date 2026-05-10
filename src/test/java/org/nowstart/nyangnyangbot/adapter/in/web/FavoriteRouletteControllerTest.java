package org.nowstart.nyangnyangbot.adapter.in.web;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.BDDMockito.given;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.application.roulette.dto.RouletteRunDto;
import org.nowstart.nyangnyangbot.domain.type.ConversionMode;
import org.nowstart.nyangnyangbot.domain.type.RewardType;
import org.nowstart.nyangnyangbot.domain.type.RouletteRoundStatus;
import org.nowstart.nyangnyangbot.application.service.RouletteService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class FavoriteRouletteControllerTest {

    @Mock
    private RouletteService rouletteService;

    @Test
    void getMyResults_ShouldUseAuthenticatedUserId() {
        FavoriteRouletteController controller = new FavoriteRouletteController(rouletteService);
        List<RouletteRunDto.RoundResponse> response = List.of(new RouletteRunDto.RoundResponse(
                1L,
                1,
                "호감도 +10",
                false,
                RewardType.FAVORITE,
                ConversionMode.AUTO,
                10,
                RouletteRoundStatus.APPLIED,
                99L,
                100L,
                null
        ));
        given(rouletteService.getRecentRounds("user-1", 5)).willReturn(response);

        ResponseEntity<List<RouletteRunDto.RoundResponse>> result = controller.getMyResults(
                new UsernamePasswordAuthenticationToken("user-1", "N/A"),
                5
        );

        then(result.getBody()).isEqualTo(response);
        BDDMockito.then(rouletteService).should().getRecentRounds("user-1", 5);
    }
}
