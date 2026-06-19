package org.nowstart.nyangnyangbot.adapter.in.web.roulette;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.BDDMockito.given;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.adapter.in.web.roulette.request.RouletteItemRequest;
import org.nowstart.nyangnyangbot.adapter.in.web.roulette.request.RouletteTableCreateRequest;
import org.nowstart.nyangnyangbot.adapter.in.web.roulette.response.RouletteEventPageResponse;
import org.nowstart.nyangnyangbot.adapter.in.web.roulette.response.RouletteEventSummaryResponse;
import org.nowstart.nyangnyangbot.adapter.in.web.roulette.response.RouletteItemResponse;
import org.nowstart.nyangnyangbot.adapter.in.web.roulette.response.RouletteTableResponse;
import org.nowstart.nyangnyangbot.application.port.in.roulette.ManageRouletteUseCase;
import org.nowstart.nyangnyangbot.application.port.in.roulette.ManageRouletteUseCase.RouletteItemResult;
import org.nowstart.nyangnyangbot.application.port.in.roulette.ManageRouletteUseCase.RouletteTableResult;
import org.nowstart.nyangnyangbot.application.port.in.roulette.ManageRouletteUseCase.RouletteValidationResult;
import org.nowstart.nyangnyangbot.application.port.in.roulette.QueryRouletteResultUseCase;
import org.nowstart.nyangnyangbot.application.port.in.roulette.QueryRouletteResultUseCase.RouletteEventSummaryResult;
import org.nowstart.nyangnyangbot.domain.type.ConversionMode;
import org.nowstart.nyangnyangbot.domain.type.RewardType;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class AdminRouletteControllerTest {

    @Mock
    private ManageRouletteUseCase rouletteService;

    @Mock
    private QueryRouletteResultUseCase rouletteQueryService;

    @Test
    @DisplayName("룰렛 테이블 생성 요청을 관리 유스케이스에 위임한다")
    void createTable_ShouldDelegateToManageUseCase() {
        // 준비
        AdminRouletteController controller = new AdminRouletteController(rouletteService, rouletteQueryService);
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

        // 실행
        ResponseEntity<RouletteTableResponse> result = controller.createTable(request);

        // 검증
        then(result.getBody()).isEqualTo(RouletteTableResponse.from(table));
        BDDMockito.then(rouletteService).should().createTable("기본 룰렛", "!룰렛", 1_000L, 100);
    }

    @Test
    @DisplayName("룰렛 아이템 추가 요청을 관리 유스케이스에 위임한다")
    void addItem_ShouldDelegateToManageUseCase() {
        // 준비
        AdminRouletteController controller = new AdminRouletteController(rouletteService, rouletteQueryService);
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

        // 실행
        ResponseEntity<RouletteItemResponse> result = controller.addItem(1L, request);

        // 검증
        then(result.getBody()).isEqualTo(RouletteItemResponse.from(item));
        BDDMockito.then(rouletteService).should().addItem(1L, "꽝", 1_000, true, RewardType.CUSTOM.name(), ConversionMode.NONE.name(), null, 1);
    }

    @Test
    @DisplayName("최근 룰렛 실행 목록을 5개 페이지 기준으로 조회한다")
    void getEvents_ShouldDelegateToQueryUseCase() {
        // 준비
        AdminRouletteController controller = new AdminRouletteController(rouletteService, rouletteQueryService);
        PageRequest pageable = PageRequest.of(0, 5);
        RouletteEventSummaryResult event = new RouletteEventSummaryResult(
                10L,
                "donation-1",
                "user-1",
                "치즈냥",
                5_000L,
                5,
                "APPLIED",
                LocalDateTime.of(2026, 6, 19, 15, 30)
        );
        given(rouletteQueryService.getRecentEvents(pageable))
                .willReturn(new PageImpl<>(List.of(event), pageable, 1));

        // 실행
        ResponseEntity<RouletteEventPageResponse> result = controller.getEvents(0, 5);

        // 검증
        then(result.getBody()).isNotNull();
        then(result.getBody().content()).containsExactly(RouletteEventSummaryResponse.from(event));
        then(result.getBody().number()).isZero();
        then(result.getBody().size()).isEqualTo(5);
        BDDMockito.then(rouletteQueryService).should().getRecentEvents(pageable);
    }
}
