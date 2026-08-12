package org.nowstart.nyangnyangbot.application.port.in.roulette;

import static org.nowstart.nyangnyangbot.domain.roulette.RoulettePolicy.TOTAL_PROBABILITY;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import org.nowstart.nyangnyangbot.domain.chat.CommandTrigger;
import org.nowstart.nyangnyangbot.domain.roulette.RoulettePolicy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ManageRouletteUseCase {

    int DEFAULT_SIMULATION_ITERATIONS = RoulettePolicy.DEFAULT_SIMULATION_ITERATIONS;
    int MIN_SIMULATION_ITERATIONS = RoulettePolicy.MIN_SIMULATION_ITERATIONS;
    int MAX_SIMULATION_ITERATIONS = RoulettePolicy.MAX_SIMULATION_ITERATIONS;
    int MIN_TRIGGER_LENGTH = CommandTrigger.MIN_LENGTH;
    int MAX_TRIGGER_LENGTH = CommandTrigger.MAX_LENGTH;
    String TRIGGER_LENGTH_MESSAGE = CommandTrigger.LENGTH_MESSAGE;

    /** 초안 상태의 룰렛 설정을 생성한다. */
    RouletteConfigResult createConfig(@Valid @NotNull CreateRouletteConfigCommand command);

    /** 초안 상태의 룰렛 설정에 선택지를 추가한다. */
    RouletteOptionResult addOption(@Valid @NotNull AddRouletteOptionCommand command);

    /** 룰렛 설정을 최신 생성 순으로 반환한다. */
    Page<RouletteConfigSummaryResult> getConfigs(
            @NotNull(message = "pageable is required") Pageable pageable
    );

    /** 지정한 룰렛 설정과 선택지를 반환한다. */
    RouletteConfigResult getConfig(
            @NotNull(message = "configId is required")
            @Positive(message = "configId must be positive") Long configId
    );

    /** 초안 설정을 활성화하고 이전 활성 설정을 보관 상태로 변경한다. */
    RouletteConfigResult activateConfig(
            @NotNull(message = "configId is required")
            @Positive(message = "configId must be positive") Long configId
    );

    /** 지정한 룰렛 설정을 보관 상태로 변경한다. */
    RouletteConfigResult archiveConfig(
            @NotNull(message = "configId is required")
            @Positive(message = "configId must be positive") Long configId
    );

    /** 룰렛 실행이나 보상을 생성하지 않고 지정한 설정을 시뮬레이션한다. */
    RouletteSimulationResult simulate(
            @NotNull(message = "configId is required")
            @Positive(message = "configId must be positive") Long configId,
            @Min(value = MIN_SIMULATION_ITERATIONS, message = "iterations must be at least 1")
            @Max(value = MAX_SIMULATION_ITERATIONS, message = "iterations must be 10000 or less") int iterations
    );

    record CreateRouletteConfigCommand(
            @NotBlank @Size(max = 100) String title,
            @NotBlank
            @Size(min = MIN_TRIGGER_LENGTH, max = MAX_TRIGGER_LENGTH, message = TRIGGER_LENGTH_MESSAGE)
            String triggerToken,
            @NotNull @Positive Long pricePerRound,
            @Positive Integer highRoundThreshold
    ) {
    }

    record AddRouletteOptionCommand(
            @NotNull @Positive Long configId,
            @NotBlank @Size(max = 100) String label,
            @NotNull @Min(0) @Max(TOTAL_PROBABILITY) Integer probabilityBasisPoints,
            Boolean losing,
            String rewardType,
            String conversionMode,
            Long pointDelta,
            @PositiveOrZero Integer displayOrder
    ) {
        public int displayOrderOrDefault() {
            return displayOrder == null ? 0 : displayOrder;
        }
    }

    record RouletteConfigResult(
            Long id,
            String title,
            String triggerToken,
            Long pricePerRound,
            String status,
            Integer highRoundThreshold,
            RouletteValidationResult validation,
            List<RouletteOptionResult> options,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    record RouletteOptionResult(
            Long id,
            String label,
            Integer probabilityBasisPoints,
            Boolean losing,
            String rewardType,
            String conversionMode,
            Long pointDelta,
            Integer displayOrder
    ) {
    }

    record RouletteConfigSummaryResult(
            Long id,
            String title,
            String triggerToken,
            Long pricePerRound,
            String status,
            Instant createdAt
    ) {
    }

    record RouletteValidationResult(
            Boolean activatable,
            List<String> reasons,
            Integer probabilityTotal,
            Boolean hasLosingOption
    ) {
    }

    record RouletteSimulationResult(Integer iterations, List<Entry> options) {

        public record Entry(String label, Integer count, Double ratio) {
        }
    }
}
