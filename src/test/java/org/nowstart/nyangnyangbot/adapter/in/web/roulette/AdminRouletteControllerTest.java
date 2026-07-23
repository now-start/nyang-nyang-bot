package org.nowstart.nyangnyangbot.adapter.in.web.roulette;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.BDDMockito.given;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.adapter.in.web.roulette.AdminRouletteController.RouletteConfigForm;
import org.nowstart.nyangnyangbot.adapter.in.web.roulette.AdminRouletteController.RouletteOptionForm;
import org.nowstart.nyangnyangbot.application.port.in.roulette.ManageRouletteUseCase;
import org.nowstart.nyangnyangbot.application.port.in.roulette.ManageRouletteUseCase.AddRouletteOptionCommand;
import org.nowstart.nyangnyangbot.application.port.in.roulette.ManageRouletteUseCase.CreateRouletteConfigCommand;
import org.nowstart.nyangnyangbot.application.port.in.roulette.ManageRouletteUseCase.RouletteConfigResult;
import org.nowstart.nyangnyangbot.application.port.in.roulette.ManageRouletteUseCase.RouletteConfigSummaryResult;
import org.nowstart.nyangnyangbot.application.port.in.roulette.ManageRouletteUseCase.RouletteOptionResult;
import org.nowstart.nyangnyangbot.application.port.in.roulette.ManageRouletteUseCase.RouletteValidationResult;
import org.nowstart.nyangnyangbot.application.port.in.roulette.QueryRouletteResultUseCase;
import org.nowstart.nyangnyangbot.application.port.in.roulette.QueryRouletteResultUseCase.RouletteRunSummaryResult;
import org.nowstart.nyangnyangbot.domain.type.ConversionMode;
import org.nowstart.nyangnyangbot.domain.type.RewardType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.ui.ConcurrentModel;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;

@ExtendWith(MockitoExtension.class)
class AdminRouletteControllerTest {

    @Mock
    private ManageRouletteUseCase rouletteService;

    @Mock
    private QueryRouletteResultUseCase rouletteQueryService;

    @Test
    @DisplayName("룰렛 설정 생성 요청을 관리 유스케이스에 위임하고 설정 fragment를 반환한다")
    void createConfig_ShouldDelegateToManageUseCaseAndReturnConfigFragment() {
        AdminRouletteController controller = new AdminRouletteController(rouletteService, rouletteQueryService);
        RouletteConfigForm form = new RouletteConfigForm("기본 룰렛", "!룰렛", 1_000L, 100);
        RouletteConfigResult config = config(1L, "기본 룰렛", "!룰렛", 1_000L, List.of());
        CreateRouletteConfigCommand command = new CreateRouletteConfigCommand("기본 룰렛", "!룰렛", 1_000L, 100);
        given(rouletteService.createConfig(command)).willReturn(config);
        given(rouletteService.getConfigs(PageRequest.of(0, 20))).willReturn(configPage(config));
        given(rouletteService.getConfig(1L)).willReturn(config);
        ConcurrentModel model = new ConcurrentModel();

        String view = controller.createConfig(form, bindingResult(form), model);

        then(view).isEqualTo("features/roulette/components :: roulette-config-region");
        then(model.getAttribute("selectedConfigId")).isEqualTo(1L);
        then(model.getAttribute("config")).isEqualTo(config);
        then(model.getAttribute("rouletteTone")).isEqualTo("success");
        BDDMockito.then(rouletteService).should().createConfig(command);
    }

    @Test
    @DisplayName("룰렛 옵션 추가 요청을 관리 유스케이스에 위임하고 설정 fragment를 반환한다")
    void addOption_ShouldDelegateToManageUseCaseAndReturnConfigFragment() {
        AdminRouletteController controller = new AdminRouletteController(rouletteService, rouletteQueryService);
        RouletteOptionForm form = new RouletteOptionForm(
                1L,
                "꽝",
                new BigDecimal("10"),
                true,
                RewardType.CUSTOM.name(),
                ConversionMode.NONE.name(),
                null
        );
        RouletteConfigResult config = config(1L, "기본 룰렛", "!룰렛", 1_000L, List.of());
        RouletteOptionResult option = new RouletteOptionResult(
                10L,
                "꽝",
                1_000,
                true,
                RewardType.CUSTOM.name(),
                ConversionMode.NONE.name(),
                null,
                1
        );
        given(rouletteService.getConfigs(PageRequest.of(0, 20))).willReturn(configPage(config));
        given(rouletteService.getConfig(1L)).willReturn(config);
        AddRouletteOptionCommand command = new AddRouletteOptionCommand(
                1L, "꽝", 1_000, true, RewardType.CUSTOM.name(), ConversionMode.NONE.name(), null, 1
        );
        given(rouletteService.addOption(command)).willReturn(option);
        ConcurrentModel model = new ConcurrentModel();

        String view = controller.addOption(form, bindingResult(form), model);

        then(view).isEqualTo("features/roulette/components :: roulette-config-region");
        then(model.getAttribute("config")).isEqualTo(config);
        then(model.getAttribute("selectedConfigId")).isEqualTo(1L);
        then(model.getAttribute("rouletteTone")).isEqualTo("success");
        BDDMockito.then(rouletteService).should().addOption(command);
    }

    @Test
    @DisplayName("최근 룰렛 실행 목록을 5개 페이지 기준으로 조회한다")
    void getRuns_ShouldDelegateToQueryUseCase() {
        AdminRouletteController controller = new AdminRouletteController(rouletteService, rouletteQueryService);
        PageRequest pageable = PageRequest.of(0, 5);
        RouletteRunSummaryResult run = new RouletteRunSummaryResult(
                10L,
                "donation-1",
                "user-1",
                "치즈냥",
                5_000L,
                5,
                "APPLIED",
                Instant.parse("2026-06-19T06:30:00Z")
        );
        Page<RouletteRunSummaryResult> page = new PageImpl<>(List.of(run), pageable, 1);
        given(rouletteQueryService.getRecentRuns(pageable)).willReturn(page);
        ConcurrentModel model = new ConcurrentModel();

        String view = controller.getRuns(0, 5, model);

        then(view).isEqualTo("features/roulette/components :: roulette-runs");
        then(model.getAttribute("runsPage")).isEqualTo(page);
        BDDMockito.then(rouletteQueryService).should().getRecentRuns(pageable);
    }

    private RouletteConfigResult config(
            Long id,
            String title,
            String triggerToken,
            Long pricePerRound,
            List<RouletteOptionResult> options
    ) {
        RouletteValidationResult validation =
                new RouletteValidationResult(false, List.of("losing option is required"), 0, false);
        Instant createdAt = Instant.parse("2026-06-19T06:00:00Z");
        return new RouletteConfigResult(
                id,
                title,
                triggerToken,
                pricePerRound,
                "DRAFT",
                100,
                validation,
                options,
                createdAt,
                createdAt
        );
    }

    private Page<RouletteConfigSummaryResult> configPage(RouletteConfigResult config) {
        RouletteConfigSummaryResult summary = new RouletteConfigSummaryResult(
                config.id(),
                config.title(),
                config.triggerToken(),
                config.pricePerRound(),
                config.status(),
                config.createdAt()
        );
        PageRequest pageable = PageRequest.of(0, 20);
        return new PageImpl<>(List.of(summary), pageable, 1);
    }

    private BindingResult bindingResult(Object form) {
        return new BeanPropertyBindingResult(form, "form");
    }
}
