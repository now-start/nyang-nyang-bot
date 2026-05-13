package org.nowstart.nyangnyangbot.adapter.in.web.roulette;

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
import org.nowstart.nyangnyangbot.application.port.in.roulette.ManageRouletteUseCase;
import org.nowstart.nyangnyangbot.application.port.in.roulette.ManageRouletteUseCase.RouletteItemResult;
import org.nowstart.nyangnyangbot.application.port.in.roulette.ManageRouletteUseCase.RouletteTableResult;
import org.nowstart.nyangnyangbot.application.port.in.roulette.ManageRouletteUseCase.RouletteValidationResult;
import org.nowstart.nyangnyangbot.domain.type.ConversionMode;
import org.nowstart.nyangnyangbot.domain.type.RewardType;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class AdminRouletteControllerTest {

    @Mock
    private ManageRouletteUseCase rouletteService;

    @Test
    void createTable_ShouldDelegateToManageUseCase() {
        AdminRouletteController controller = new AdminRouletteController(rouletteService);
        RouletteTableCreateRequest request = new RouletteTableCreateRequest(
                "기본 룰렛",
                "!룰렛",
                1_000L,
                100
        );
        RouletteValidationResult validation =
                new RouletteValidationResult(false, List.of("losing item is required"), 0, false);
        RouletteTableResult table = new RouletteTableResult(
                1L, "기본 룰렛", "!룰렛", 1_000L, false, 0, 100, validation, List.of()
        );
        given(rouletteService.createTable("기본 룰렛", "!룰렛", 1_000L, 100)).willReturn(table);

        ResponseEntity<RouletteTableResponse> result = controller.createTable(request);

        then(result.getBody()).isEqualTo(RouletteTableResponse.from(table));
        BDDMockito.then(rouletteService).should().createTable("기본 룰렛", "!룰렛", 1_000L, 100);
    }

    @Test
    void addItem_ShouldDelegateToManageUseCase() {
        AdminRouletteController controller = new AdminRouletteController(rouletteService);
        RouletteItemRequest request = new RouletteItemRequest(
                "꽝",
                1_000,
                true,
                RewardType.CUSTOM.name(),
                ConversionMode.NONE.name(),
                null,
                1
        );
        RouletteItemResult item = new RouletteItemResult(
                10L,
                "꽝",
                1_000,
                true,
                RewardType.CUSTOM.name(),
                ConversionMode.NONE.name(),
                null,
                true,
                1
        );
        given(rouletteService.addItem(1L, "꽝", 1_000, true, RewardType.CUSTOM.name(), ConversionMode.NONE.name(), null, 1))
                .willReturn(item);

        ResponseEntity<RouletteItemResponse> result = controller.addItem(1L, request);

        then(result.getBody()).isEqualTo(RouletteItemResponse.from(item));
        BDDMockito.then(rouletteService).should().addItem(1L, "꽝", 1_000, true, RewardType.CUSTOM.name(), ConversionMode.NONE.name(), null, 1);
    }
}
