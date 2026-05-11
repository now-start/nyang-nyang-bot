package org.nowstart.nyangnyangbot.adapter.in.web;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.BDDMockito.given;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.adapter.in.web.roulette.request.RouletteItemRequest;
import org.nowstart.nyangnyangbot.adapter.in.web.roulette.request.RouletteTableCreateRequest;
import org.nowstart.nyangnyangbot.adapter.in.web.roulette.response.RouletteItemResponse;
import org.nowstart.nyangnyangbot.adapter.in.web.roulette.response.RouletteTableResponse;
import org.nowstart.nyangnyangbot.application.port.in.roulette.dto.RouletteTableSnapshot;
import org.nowstart.nyangnyangbot.application.port.in.roulette.dto.RouletteValidationResult;
import org.nowstart.nyangnyangbot.application.service.roulette.RouletteService;
import org.nowstart.nyangnyangbot.domain.model.RouletteItem;
import org.nowstart.nyangnyangbot.domain.model.RouletteTable;
import org.nowstart.nyangnyangbot.domain.type.ConversionMode;
import org.nowstart.nyangnyangbot.domain.type.RewardType;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class AdminRouletteControllerTest {

    @Mock
    private RouletteService rouletteService;

    @Test
    void createTable_ShouldDelegateToRouletteService() {
        AdminRouletteController controller = new AdminRouletteController(rouletteService);
        RouletteTableCreateRequest request = new RouletteTableCreateRequest(
                "기본 룰렛",
                "!룰렛",
                1_000L,
                100
        );
        RouletteTable table = new RouletteTable(1L, "기본 룰렛", "!룰렛", 1_000L, false, 0, 100);
        RouletteValidationResult validation =
                new RouletteValidationResult(false, List.of("losing item is required"), 0, false);
        RouletteTableSnapshot snapshot = new RouletteTableSnapshot(table, List.of(), validation);
        given(rouletteService.createTable("기본 룰렛", "!룰렛", 1_000L, 100)).willReturn(snapshot);

        ResponseEntity<RouletteTableResponse> result = controller.createTable(request);

        then(result.getBody()).isEqualTo(RouletteTableResponse.from(snapshot));
        BDDMockito.then(rouletteService).should().createTable("기본 룰렛", "!룰렛", 1_000L, 100);
    }

    @Test
    void addItem_ShouldDelegateToRouletteService() {
        AdminRouletteController controller = new AdminRouletteController(rouletteService);
        RouletteItemRequest request = new RouletteItemRequest(
                "꽝",
                1_000,
                true,
                RewardType.CUSTOM,
                ConversionMode.NONE,
                null,
                1
        );
        RouletteItem item = new RouletteItem(
                10L,
                1L,
                "꽝",
                1_000,
                true,
                RewardType.CUSTOM,
                ConversionMode.NONE,
                null,
                true,
                1
        );
        given(rouletteService.addItem(1L, "꽝", 1_000, true, RewardType.CUSTOM, ConversionMode.NONE, null, 1))
                .willReturn(item);

        ResponseEntity<RouletteItemResponse> result = controller.addItem(1L, request);

        then(result.getBody()).isEqualTo(RouletteItemResponse.from(item));
        BDDMockito.then(rouletteService).should().addItem(1L, "꽝", 1_000, true, RewardType.CUSTOM, ConversionMode.NONE, null, 1);
    }
}
