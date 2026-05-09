package org.nowstart.nyangnyangbot.controller;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.BDDMockito.given;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.data.dto.roulette.RouletteTableDto;
import org.nowstart.nyangnyangbot.data.type.ConversionMode;
import org.nowstart.nyangnyangbot.data.type.RewardType;
import org.nowstart.nyangnyangbot.service.RouletteService;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class AdminRouletteControllerTest {

    @Mock
    private RouletteService rouletteService;

    @Test
    void createTable_ShouldDelegateToRouletteService() {
        AdminRouletteController controller = new AdminRouletteController(rouletteService);
        RouletteTableDto.CreateRequest request = new RouletteTableDto.CreateRequest(
                "기본 룰렛",
                "!룰렛",
                1_000L,
                100
        );
        RouletteTableDto.Response response = new RouletteTableDto.Response(
                1L,
                "기본 룰렛",
                "!룰렛",
                1_000L,
                false,
                0,
                100,
                new RouletteTableDto.ValidationResponse(false, List.of("losing item is required"), 0, false),
                List.of()
        );
        given(rouletteService.createTable(request)).willReturn(response);

        ResponseEntity<RouletteTableDto.Response> result = controller.createTable(request);

        then(result.getBody()).isEqualTo(response);
        BDDMockito.then(rouletteService).should().createTable(request);
    }

    @Test
    void addItem_ShouldDelegateToRouletteService() {
        AdminRouletteController controller = new AdminRouletteController(rouletteService);
        RouletteTableDto.ItemRequest request = new RouletteTableDto.ItemRequest(
                "꽝",
                1_000,
                true,
                RewardType.CUSTOM,
                ConversionMode.NONE,
                null,
                1
        );
        RouletteTableDto.ItemResponse response = new RouletteTableDto.ItemResponse(
                10L,
                "꽝",
                1_000,
                true,
                RewardType.CUSTOM,
                ConversionMode.NONE,
                null,
                true,
                1
        );
        given(rouletteService.addItem(1L, request)).willReturn(response);

        ResponseEntity<RouletteTableDto.ItemResponse> result = controller.addItem(1L, request);

        then(result.getBody()).isEqualTo(response);
        BDDMockito.then(rouletteService).should().addItem(1L, request);
    }
}
