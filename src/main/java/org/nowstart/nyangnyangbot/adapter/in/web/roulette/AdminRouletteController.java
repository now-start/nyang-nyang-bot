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
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.application.port.in.roulette.ManageRouletteUseCase;
import org.nowstart.nyangnyangbot.application.port.in.roulette.ManageRouletteUseCase.AddRouletteItemCommand;
import org.nowstart.nyangnyangbot.application.port.in.roulette.ManageRouletteUseCase.CreateRouletteTableCommand;
import org.nowstart.nyangnyangbot.application.port.in.roulette.ManageRouletteUseCase.RouletteTableResult;
import org.nowstart.nyangnyangbot.application.port.in.roulette.QueryRouletteResultUseCase;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.GetMapping;
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
    private static final String EVENTS_FRAGMENT = "features/roulette/components :: roulette-events";
    private static final String SIMULATION_FRAGMENT = "features/roulette/components :: roulette-simulation";

    private final ManageRouletteUseCase manageRouletteUseCase;
    private final QueryRouletteResultUseCase queryRouletteResultUseCase;

    @Operation(summary = "룰렛 테이블 조회")
    @GetMapping("/tables")
    public String getTables(
            @RequestParam(required = false) Long selectedTableId,
            Model model
    ) {
        addRouletteConfig(model, selectedTableId);
        return CONFIG_FRAGMENT;
    }

    @Operation(summary = "최근 룰렛 실행 목록 조회")
    @GetMapping("/events")
    public String getEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            Model model
    ) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 20);
        Pageable pageable = PageRequest.of(safePage, safeSize);
        model.addAttribute("eventsPage", queryRouletteResultUseCase.getRecentEvents(pageable));
        return EVENTS_FRAGMENT;
    }

    @Operation(summary = "룰렛 테이블 상세 조회")
    @GetMapping("/tables/{tableId}/detail")
    public String getTableDetail(
            @PathVariable Long tableId,
            Model model
    ) {
        addRouletteConfig(model, tableId);
        return CONFIG_FRAGMENT;
    }

    @Operation(summary = "룰렛 테이블 생성")
    @PostMapping("/tables")
    public String createTable(
            @Valid @ModelAttribute RouletteTableForm form,
            BindingResult bindingResult,
            Model model
    ) {
        Long selectedTableId = null;
        if (bindingResult.hasErrors()) {
            model.addAttribute("rouletteMessage", "룰렛 생성에 실패했습니다.");
            model.addAttribute("rouletteTone", "danger");
        } else {
            try {
                RouletteTableResult table = manageRouletteUseCase.createTable(new CreateRouletteTableCommand(
                        form.title(),
                        form.command(),
                        form.pricePerRound(),
                        form.highRoundThresholdOrDefault()
                ));
                selectedTableId = table.id();
                model.addAttribute("rouletteMessage", "룰렛 생성 완료");
                model.addAttribute("rouletteTone", "success");
            } catch (RuntimeException e) {
                log.warn("Failed to create roulette table. title={}, command={}", form.title(), form.command(), e);
                model.addAttribute("rouletteMessage", "룰렛 생성에 실패했습니다.");
                model.addAttribute("rouletteTone", "danger");
            }
        }
        addRouletteConfig(model, selectedTableId);
        return CONFIG_FRAGMENT;
    }

    @Operation(summary = "룰렛 항목 추가")
    @PostMapping("/items")
    public String addItem(
            @Valid @ModelAttribute RouletteItemForm form,
            BindingResult bindingResult,
            Model model
    ) {
        Long selectedTableId = form.tableId();
        if (bindingResult.hasErrors()) {
            model.addAttribute("rouletteMessage", "룰렛 항목 추가에 실패했습니다.");
            model.addAttribute("rouletteTone", "danger");
        } else {
            try {
                RouletteTableResult table = requireTable(selectedTableId);
                manageRouletteUseCase.addItem(new AddRouletteItemCommand(
                        selectedTableId,
                        form.label(),
                        form.probabilityBasisPoints(),
                        form.losingItemOrDefault(),
                        form.rewardTypeOrDefault(),
                        form.conversionModeOrDefault(),
                        form.exchangeFavoriteValue(),
                        table.items().size() + 1
                ));
                model.addAttribute("rouletteMessage", "룰렛 항목 추가 완료");
                model.addAttribute("rouletteTone", "success");
            } catch (RuntimeException e) {
                log.warn("Failed to add roulette item. tableId={}, label={}", selectedTableId, form.label(), e);
                model.addAttribute("rouletteMessage", "룰렛 항목 추가에 실패했습니다.");
                model.addAttribute("rouletteTone", "danger");
            }
        }
        addRouletteConfig(model, selectedTableId);
        return CONFIG_FRAGMENT;
    }

    @Operation(summary = "룰렛 테이블 활성화")
    @PostMapping("/tables/{tableId}/activate")
    public String activateTable(
            @PathVariable Long tableId,
            Model model
    ) {
        try {
            manageRouletteUseCase.activateTable(tableId);
            model.addAttribute("rouletteMessage", "룰렛 활성화 완료");
            model.addAttribute("rouletteTone", "success");
        } catch (RuntimeException e) {
            log.warn("Failed to activate roulette table. tableId={}", tableId, e);
            model.addAttribute("rouletteMessage", "룰렛 활성화에 실패했습니다.");
            model.addAttribute("rouletteTone", "danger");
        }
        addRouletteConfig(model, tableId);
        return CONFIG_FRAGMENT;
    }

    @Operation(summary = "룰렛 테이블 비활성화")
    @PostMapping("/tables/{tableId}/deactivate")
    public String deactivateTable(
            @PathVariable Long tableId,
            Model model
    ) {
        try {
            manageRouletteUseCase.deactivateTable(tableId);
            model.addAttribute("rouletteMessage", "룰렛 비활성화 완료");
            model.addAttribute("rouletteTone", "success");
        } catch (RuntimeException e) {
            log.warn("Failed to deactivate roulette table. tableId={}", tableId, e);
            model.addAttribute("rouletteMessage", "룰렛 비활성화에 실패했습니다.");
            model.addAttribute("rouletteTone", "danger");
        }
        addRouletteConfig(model, tableId);
        return CONFIG_FRAGMENT;
    }

    @Operation(summary = "룰렛 확률 시뮬레이션")
    @GetMapping("/tables/{tableId}/simulation")
    public String simulate(
            @PathVariable Long tableId,
            @RequestParam(defaultValue = "10000") int iterations,
            Model model
    ) {
        model.addAttribute("simulation", manageRouletteUseCase.simulate(tableId, iterations));
        return SIMULATION_FRAGMENT;
    }

    private void addRouletteConfig(Model model, Long selectedTableId) {
        List<RouletteTableResult> tables = manageRouletteUseCase.getTables();
        Long effectiveSelectedTableId = effectiveSelectedTableId(tables, selectedTableId);
        model.addAttribute("tables", tables);
        model.addAttribute("selectedTableId", effectiveSelectedTableId);
        model.addAttribute("table", findTable(tables, effectiveSelectedTableId));
    }

    private RouletteTableResult requireTable(Long tableId) {
        RouletteTableResult table = findTable(tableId);
        if (table == null) {
            throw new IllegalArgumentException("roulette table is required");
        }
        return table;
    }

    private RouletteTableResult findTable(Long tableId) {
        if (tableId == null) {
            return null;
        }
        return manageRouletteUseCase.getTables().stream()
                .filter(table -> tableId.equals(table.id()))
                .findFirst()
                .orElse(null);
    }

    private RouletteTableResult findTable(List<RouletteTableResult> tables, Long tableId) {
        if (tableId == null) {
            return null;
        }
        return tables.stream()
                .filter(table -> tableId.equals(table.id()))
                .findFirst()
                .orElse(null);
    }

    private Long effectiveSelectedTableId(List<RouletteTableResult> tables, Long selectedTableId) {
        if (tables.isEmpty()) {
            return null;
        }
        if (selectedTableId != null && tables.stream().anyMatch(table -> selectedTableId.equals(table.id()))) {
            return selectedTableId;
        }
        return tables.getFirst().id();
    }

    public record RouletteTableForm(
            @NotBlank(message = "title is required")
            @Size(max = 255, message = "title length must be 255 or less")
            String title,
            @NotBlank(message = "command is required")
            @Size(max = 255, message = "command length must be 255 or less")
            String command,
            @NotNull(message = "pricePerRound is required")
            @Positive(message = "pricePerRound must be positive")
            Long pricePerRound,
            @Positive(message = "highRoundThreshold must be positive")
            Integer highRoundThreshold
    ) {

        Integer highRoundThresholdOrDefault() {
            return highRoundThreshold == null ? 100 : highRoundThreshold;
        }
    }

    public record RouletteItemForm(
            @NotNull(message = "tableId is required")
            @Positive(message = "tableId must be positive")
            Long tableId,
            @NotBlank(message = "label is required")
            @Size(max = 255, message = "label length must be 255 or less")
            String label,
            @NotNull(message = "probabilityPercent is required")
            @DecimalMin(value = "0", message = "probabilityPercent must be between 0 and 100")
            @DecimalMax(value = "100", message = "probabilityPercent must be between 0 and 100")
            BigDecimal probabilityPercent,
            Boolean losingItem,
            String rewardType,
            String conversionMode,
            Integer exchangeFavoriteValue
    ) {

        Integer probabilityBasisPoints() {
            if (probabilityPercent == null) {
                return null;
            }
            return probabilityPercent.movePointRight(2)
                    .setScale(0, RoundingMode.HALF_UP)
                    .intValueExact();
        }

        Boolean losingItemOrDefault() {
            return Boolean.TRUE.equals(losingItem);
        }

        String rewardTypeOrDefault() {
            return Boolean.TRUE.equals(losingItem) ? "CUSTOM" : rewardType;
        }

        String conversionModeOrDefault() {
            return Boolean.TRUE.equals(losingItem) ? "NONE" : conversionMode;
        }
    }
}
