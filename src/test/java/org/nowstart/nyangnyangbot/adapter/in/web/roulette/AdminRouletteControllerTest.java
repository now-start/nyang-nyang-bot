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
import org.nowstart.nyangnyangbot.adapter.in.web.roulette.AdminRouletteController.RouletteItemForm;
import org.nowstart.nyangnyangbot.adapter.in.web.roulette.AdminRouletteController.RouletteTableForm;
import org.nowstart.nyangnyangbot.application.port.in.roulette.ManageRouletteUseCase;
import org.nowstart.nyangnyangbot.application.port.in.roulette.ManageRouletteUseCase.RouletteItemResult;
import org.nowstart.nyangnyangbot.application.port.in.roulette.ManageRouletteUseCase.RouletteTableResult;
import org.nowstart.nyangnyangbot.application.port.in.roulette.ManageRouletteUseCase.RouletteValidationResult;
import org.nowstart.nyangnyangbot.application.port.in.roulette.QueryRouletteResultUseCase;
import org.nowstart.nyangnyangbot.application.port.in.roulette.QueryRouletteResultUseCase.RouletteEventSummaryResult;
import org.nowstart.nyangnyangbot.domain.type.ConversionMode;
import org.nowstart.nyangnyangbot.domain.type.RewardType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.ui.ConcurrentModel;

@ExtendWith(MockitoExtension.class)
class AdminRouletteControllerTest {

    @Mock
    private ManageRouletteUseCase rouletteService;

    @Mock
    private QueryRouletteResultUseCase rouletteQueryService;

    @Test
    @DisplayName("룰렛 테이블 생성 요청을 관리 유스케이스에 위임하고 설정 fragment를 반환한다")
    void createTable_ShouldDelegateToManageUseCaseAndReturnConfigFragment() {
        // 준비
        AdminRouletteController controller = new AdminRouletteController(rouletteService, rouletteQueryService);
        RouletteTableForm form = new RouletteTableForm("기본 룰렛", "!룰렛", 1_000L, 100);
        RouletteTableResult table = table(1L, "기본 룰렛", "!룰렛", 1_000L, List.of());
        given(rouletteService.createTable("기본 룰렛", "!룰렛", 1_000L, 100)).willReturn(table);
        given(rouletteService.getTables()).willReturn(List.of(table));
        ConcurrentModel model = new ConcurrentModel();

        // 실행
        String view = controller.createTable(form, model);

        // 검증
        then(view).isEqualTo("features/roulette/components :: roulette-config-region");
        then(model.getAttribute("selectedTableId")).isEqualTo(1L);
        then(model.getAttribute("table")).isEqualTo(table);
        then(model.getAttribute("rouletteTone")).isEqualTo("success");
        BDDMockito.then(rouletteService).should().createTable("기본 룰렛", "!룰렛", 1_000L, 100);
    }

    @Test
    @DisplayName("룰렛 아이템 추가 요청을 관리 유스케이스에 위임하고 설정 fragment를 반환한다")
    void addItem_ShouldDelegateToManageUseCaseAndReturnConfigFragment() {
        // 준비
        AdminRouletteController controller = new AdminRouletteController(rouletteService, rouletteQueryService);
        RouletteItemForm form = new RouletteItemForm(
                1L,
                "꽝",
                "10",
                true,
                RewardType.CUSTOM.name(),
                ConversionMode.NONE.name(),
                null
        );
        RouletteTableResult table = table(1L, "기본 룰렛", "!룰렛", 1_000L, List.of());
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
        given(rouletteService.getTables()).willReturn(List.of(table));
        given(rouletteService.addItem(1L, "꽝", 1_000, true, RewardType.CUSTOM.name(), ConversionMode.NONE.name(), null, 1))
                .willReturn(item);
        ConcurrentModel model = new ConcurrentModel();

        // 실행
        String view = controller.addItem(form, model);

        // 검증
        then(view).isEqualTo("features/roulette/components :: roulette-config-region");
        then(model.getAttribute("table")).isEqualTo(table);
        then(model.getAttribute("selectedTableId")).isEqualTo(1L);
        then(model.getAttribute("rouletteTone")).isEqualTo("success");
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
        Page<RouletteEventSummaryResult> page = new PageImpl<>(List.of(event), pageable, 1);
        given(rouletteQueryService.getRecentEvents(pageable)).willReturn(page);
        ConcurrentModel model = new ConcurrentModel();

        // 실행
        String view = controller.getEvents(0, 5, model);

        // 검증
        then(view).isEqualTo("features/roulette/components :: roulette-events");
        then(model.getAttribute("eventsPage")).isEqualTo(page);
        BDDMockito.then(rouletteQueryService).should().getRecentEvents(pageable);
    }

    private RouletteTableResult table(
            Long id,
            String title,
            String command,
            Long pricePerRound,
            List<RouletteItemResult> items
    ) {
        RouletteValidationResult validation =
                new RouletteValidationResult(false, List.of("losing item is required"), 0, false);
        return new RouletteTableResult(id, title, command, pricePerRound, false, 0, 100, validation, items);
    }
}
