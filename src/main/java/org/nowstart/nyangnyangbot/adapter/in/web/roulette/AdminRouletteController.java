package org.nowstart.nyangnyangbot.adapter.in.web.roulette;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.application.port.in.roulette.ManageRouletteUseCase;
import org.nowstart.nyangnyangbot.application.port.in.roulette.ManageRouletteUseCase.AddRouletteOptionCommand;
import org.nowstart.nyangnyangbot.application.port.in.roulette.ManageRouletteUseCase.CreateRouletteConfigCommand;
import org.nowstart.nyangnyangbot.application.port.in.roulette.ManageRouletteUseCase.RouletteConfigResult;
import org.nowstart.nyangnyangbot.application.port.in.roulette.ManageRouletteUseCase.RouletteConfigSummaryResult;
import org.nowstart.nyangnyangbot.application.port.in.roulette.QueryRouletteResultUseCase;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/admin/roulette")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Roulette API", description = "관리자 후원 룰렛 API")
public class AdminRouletteController {

    private static final String CONFIG_FRAGMENT = "features/roulette/components :: roulette-config-region";
    private static final String RUNS_FRAGMENT = "features/roulette/components :: roulette-runs";
    private static final String SIMULATION_FRAGMENT = "features/roulette/components :: roulette-simulation";
    private static final int DEFAULT_CONFIG_PAGE_SIZE = 20;

    private final ManageRouletteUseCase manageRouletteUseCase;
    private final QueryRouletteResultUseCase queryRouletteResultUseCase;

    @Operation(summary = "룰렛 설정 조회")
    @GetMapping("/configs")
    public String getConfigs(
            @RequestParam(required = false) Long selectedConfigId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model
    ) {
        addRouletteConfig(model, selectedConfigId, page, size);
        return CONFIG_FRAGMENT;
    }

    @Operation(summary = "최근 룰렛 실행 목록 조회")
    @GetMapping("/runs")
    public String getRuns(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            Model model
    ) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 20);
        Pageable pageable = PageRequest.of(safePage, safeSize);
        model.addAttribute("runsPage", queryRouletteResultUseCase.getRecentRuns(pageable));
        return RUNS_FRAGMENT;
    }

    @Operation(summary = "룰렛 설정 상세 조회")
    @GetMapping("/configs/{configId}/detail")
    public String getConfigDetail(
            @PathVariable Long configId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model
    ) {
        addRouletteConfig(model, configId, page, size);
        return CONFIG_FRAGMENT;
    }

    @Operation(summary = "룰렛 설정 생성")
    @PostMapping("/configs")
    public String createConfig(
            @Valid @ModelAttribute RouletteConfigForm form,
            BindingResult bindingResult,
            Model model
    ) {
        Long selectedConfigId = null;
        if (bindingResult.hasErrors()) {
            feedback(model, "룰렛 설정 생성에 실패했습니다.", "danger");
        } else {
            try {
                RouletteConfigResult config = manageRouletteUseCase.createConfig(new CreateRouletteConfigCommand(
                        form.title(),
                        form.triggerToken(),
                        form.pricePerRound(),
                        form.highRoundThreshold()
                ));
                selectedConfigId = config.id();
                feedback(model, "룰렛 설정 생성 완료", "success");
            } catch (RuntimeException exception) {
                log.warn("Failed to create roulette config. title={}, triggerToken={}",
                        form.title(), form.triggerToken(), exception);
                feedback(model, "룰렛 설정 생성에 실패했습니다.", "danger");
            }
        }
        addRouletteConfig(model, selectedConfigId);
        return CONFIG_FRAGMENT;
    }

    @Operation(summary = "룰렛 옵션 추가")
    @PostMapping("/options")
    public String addOption(
            @Valid @ModelAttribute RouletteOptionForm form,
            BindingResult bindingResult,
            Model model
    ) {
        Long selectedConfigId = form.configId();
        if (bindingResult.hasErrors()) {
            feedback(model, "룰렛 옵션 추가에 실패했습니다.", "danger");
        } else {
            try {
                RouletteConfigResult config = requireConfig(selectedConfigId);
                manageRouletteUseCase.addOption(new AddRouletteOptionCommand(
                        selectedConfigId,
                        form.label(),
                        form.probabilityBasisPoints(),
                        form.losingOrDefault(),
                        form.rewardTypeOrDefault(),
                        form.conversionModeOrDefault(),
                        form.pointDelta(),
                        config.options().size() + 1
                ));
                feedback(model, "룰렛 옵션 추가 완료", "success");
            } catch (RuntimeException exception) {
                log.warn("Failed to add roulette option. configId={}, label={}",
                        selectedConfigId, form.label(), exception);
                feedback(model, "룰렛 옵션 추가에 실패했습니다.", "danger");
            }
        }
        addRouletteConfig(model, selectedConfigId);
        return CONFIG_FRAGMENT;
    }

    @Operation(summary = "룰렛 설정 활성화")
    @PostMapping("/configs/{configId}/activate")
    public String activateConfig(@PathVariable Long configId, Model model) {
        try {
            manageRouletteUseCase.activateConfig(configId);
            feedback(model, "룰렛 활성화 완료", "success");
        } catch (RuntimeException exception) {
            log.warn("Failed to activate roulette config. configId={}", configId, exception);
            feedback(model, "룰렛 활성화에 실패했습니다.", "danger");
        }
        addRouletteConfig(model, configId);
        return CONFIG_FRAGMENT;
    }

    @Operation(summary = "룰렛 설정 보관")
    @PostMapping("/configs/{configId}/archive")
    public String archiveConfig(@PathVariable Long configId, Model model) {
        try {
            manageRouletteUseCase.archiveConfig(configId);
            feedback(model, "룰렛 설정 보관 완료", "success");
        } catch (RuntimeException exception) {
            log.warn("Failed to archive roulette config. configId={}", configId, exception);
            feedback(model, "룰렛 설정 보관에 실패했습니다.", "danger");
        }
        addRouletteConfig(model, configId);
        return CONFIG_FRAGMENT;
    }

    @Operation(summary = "룰렛 확률 시뮬레이션")
    @GetMapping("/configs/{configId}/simulation")
    public String simulate(
            @PathVariable Long configId,
            @RequestParam(required = false) Integer iterations,
            Model model
    ) {
        int effectiveIterations = iterations == null
                ? ManageRouletteUseCase.DEFAULT_SIMULATION_ITERATIONS
                : iterations;
        model.addAttribute("simulation", manageRouletteUseCase.simulate(configId, effectiveIterations));
        return SIMULATION_FRAGMENT;
    }

    private void addRouletteConfig(Model model, Long selectedConfigId) {
        addRouletteConfig(model, selectedConfigId, 0, DEFAULT_CONFIG_PAGE_SIZE);
    }

    private void addRouletteConfig(Model model, Long selectedConfigId, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), DEFAULT_CONFIG_PAGE_SIZE);
        Page<RouletteConfigSummaryResult> configsPage = manageRouletteUseCase.getConfigs(
                PageRequest.of(safePage, safeSize)
        );
        Long effectiveSelectedConfigId = selectedConfigId != null
                ? selectedConfigId
                : configsPage.isEmpty() ? null : configsPage.getContent().getFirst().id();
        RouletteConfigResult config = findConfig(effectiveSelectedConfigId);
        model.addAttribute("configsPage", configsPage);
        model.addAttribute("configs", configsPage.getContent());
        model.addAttribute("selectedConfigId", effectiveSelectedConfigId);
        model.addAttribute("config", config);
        model.addAttribute("rouletteTriggerMaxLength", ManageRouletteUseCase.MAX_TRIGGER_LENGTH);
        model.addAttribute("defaultSimulationIterations", ManageRouletteUseCase.DEFAULT_SIMULATION_ITERATIONS);
    }

    private RouletteConfigResult requireConfig(Long configId) {
        RouletteConfigResult config = manageRouletteUseCase.getConfig(configId);
        if (config == null) {
            throw new IllegalArgumentException("roulette config is required");
        }
        return config;
    }

    private RouletteConfigResult findConfig(Long configId) {
        if (configId == null) {
            return null;
        }
        try {
            return manageRouletteUseCase.getConfig(configId);
        } catch (IllegalArgumentException missing) {
            return null;
        }
    }

    private void feedback(Model model, String message, String tone) {
        model.addAttribute("rouletteMessage", message);
        model.addAttribute("rouletteTone", tone);
    }

    public record RouletteConfigForm(
            @NotBlank(message = "title is required")
            @Size(max = 100, message = "title length must be 100 or less")
            String title,
            @NotBlank(message = "triggerToken is required")
            @Size(
                    min = ManageRouletteUseCase.MIN_TRIGGER_LENGTH,
                    max = ManageRouletteUseCase.MAX_TRIGGER_LENGTH,
                    message = ManageRouletteUseCase.TRIGGER_LENGTH_MESSAGE
            )
            String triggerToken,
            @NotNull(message = "pricePerRound is required")
            @Positive(message = "pricePerRound must be positive")
            Long pricePerRound,
            @Positive(message = "highRoundThreshold must be positive")
            Integer highRoundThreshold
    ) {

    }

    public record RouletteOptionForm(
            @NotNull(message = "configId is required")
            @Positive(message = "configId must be positive")
            Long configId,
            @NotBlank(message = "label is required")
            @Size(max = 100, message = "label length must be 100 or less")
            String label,
            @NotNull(message = "probabilityPercent is required")
            @DecimalMin(value = "0", message = "probabilityPercent must be between 0 and 100")
            @DecimalMax(value = "100", message = "probabilityPercent must be between 0 and 100")
            BigDecimal probabilityPercent,
            Boolean losing,
            String rewardType,
            String conversionMode,
            Long pointDelta
    ) {

        Integer probabilityBasisPoints() {
            if (probabilityPercent == null) {
                return null;
            }
            return probabilityPercent.movePointRight(2)
                    .setScale(0, RoundingMode.HALF_UP)
                    .intValueExact();
        }

        Boolean losingOrDefault() {
            return Boolean.TRUE.equals(losing);
        }

        String rewardTypeOrDefault() {
            return Boolean.TRUE.equals(losing) ? "CUSTOM" : rewardType;
        }

        String conversionModeOrDefault() {
            return Boolean.TRUE.equals(losing) ? "NONE" : conversionMode;
        }
    }
}
