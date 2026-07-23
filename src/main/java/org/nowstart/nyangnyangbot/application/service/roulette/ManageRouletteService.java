package org.nowstart.nyangnyangbot.application.service.roulette;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.application.port.in.roulette.ManageRouletteUseCase;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort.ConfigResult;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort.CreateConfigCommand;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort.CreateOptionCommand;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort.OptionResult;
import org.nowstart.nyangnyangbot.domain.roulette.RouletteActivationValidation;
import org.nowstart.nyangnyangbot.domain.roulette.RoulettePolicy;
import org.nowstart.nyangnyangbot.domain.type.ConversionMode;
import org.nowstart.nyangnyangbot.domain.type.RewardType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class ManageRouletteService implements ManageRouletteUseCase {

    private final RoulettePolicy roulettePolicy = new RoulettePolicy();
    private final RoulettePort roulettePort;

    @Override
    public RouletteConfigResult createConfig(CreateRouletteConfigCommand command) {
        roulettePolicy.validateConfigInput(command.title(), command.triggerToken(), command.pricePerRound());
        ConfigResult config = roulettePort.createConfig(new CreateConfigCommand(
                command.title().trim(),
                command.triggerToken().trim(),
                command.pricePerRound(),
                command.highRoundThreshold() == null
                        ? RoulettePolicy.DEFAULT_HIGH_ROUND_THRESHOLD
                        : command.highRoundThreshold(),
                now()
        ));
        return configResult(config, List.of());
    }

    @Override
    public RouletteOptionResult addOption(AddRouletteOptionCommand command) {
        RewardType rewardType = parseRewardType(command.rewardType());
        ConversionMode conversionMode = parseConversionMode(command.conversionMode());
        boolean losing = Boolean.TRUE.equals(command.losing());
        roulettePolicy.validateOptionInput(
                command.label(),
                command.probabilityBasisPoints(),
                losing,
                rewardType,
                conversionMode,
                command.pointDelta()
        );
        OptionResult option = roulettePort.addOption(new CreateOptionCommand(
                command.configId(),
                command.label().trim(),
                command.probabilityBasisPoints(),
                losing,
                rewardType,
                conversionMode,
                command.pointDelta(),
                command.displayOrder() == null ? 0 : command.displayOrder(),
                now()
        ));
        return optionResult(option);
    }

    @Override
    public Page<RouletteConfigSummaryResult> getConfigs(Pageable pageable) {
        return roulettePort.findConfigs(pageable).map(this::configSummaryResult);
    }

    @Override
    public RouletteConfigResult getConfig(Long configId) {
        ConfigResult config = requireConfig(configId);
        return configResult(config, roulettePort.findOptionsByConfigId(configId));
    }

    @Override
    public RouletteConfigResult activateConfig(Long configId) {
        ConfigResult config = requireConfig(configId);
        List<OptionResult> options = roulettePort.findOptionsByConfigId(configId);
        RouletteActivationValidation validation = roulettePolicy.validateActivation(config, options);
        if (!validation.activatable()) {
            throw new IllegalStateException(String.join(", ", validation.reasons()));
        }
        return configResult(roulettePort.activateConfig(configId, now()), options);
    }

    @Override
    public RouletteConfigResult archiveConfig(Long configId) {
        ConfigResult archived = roulettePort.archiveConfig(configId, now());
        return configResult(archived, roulettePort.findOptionsByConfigId(configId));
    }

    @Override
    public RouletteSimulationResult simulate(Long configId, int iterations) {
        List<OptionResult> options = roulettePort.findOptionsByConfigId(configId);
        RouletteActivationValidation validation = roulettePolicy.validateActivation(requireConfig(configId), options);
        if (!validation.activatable()) {
            throw new IllegalStateException("roulette config is not valid");
        }
        int safeIterations = roulettePolicy.safeSimulationIterations(iterations);
        Map<String, Integer> counts = new LinkedHashMap<>();
        options.forEach(option -> counts.put(option.label(), 0));
        for (int i = 0; i < safeIterations; i++) {
            OptionResult selected = roulettePolicy.selectOption(options);
            counts.compute(selected.label(), (label, count) -> count + 1);
        }
        return new RouletteSimulationResult(safeIterations, counts.entrySet().stream()
                .map(entry -> new RouletteSimulationResult.Entry(
                        entry.getKey(),
                        entry.getValue(),
                        entry.getValue() / (double) safeIterations
                ))
                .toList());
    }

    Instant now() {
        return Instant.now();
    }

    private RouletteConfigResult configResult(ConfigResult config, List<OptionResult> options) {
        return new RouletteConfigResult(
                config.id(),
                config.title(),
                config.triggerToken(),
                config.pricePerRound(),
                config.status().name(),
                config.highRoundThreshold(),
                validation(roulettePolicy.validateActivation(config, options)),
                options.stream().map(this::optionResult).toList(),
                config.createdAt(),
                config.updatedAt()
        );
    }

    private RouletteOptionResult optionResult(OptionResult option) {
        return new RouletteOptionResult(
                option.id(),
                option.label(),
                option.probabilityBasisPoints(),
                option.losing(),
                option.rewardType().name(),
                option.conversionMode().name(),
                option.pointDelta(),
                option.displayOrder()
        );
    }

    private RouletteConfigSummaryResult configSummaryResult(ConfigResult config) {
        return new RouletteConfigSummaryResult(
                config.id(),
                config.title(),
                config.triggerToken(),
                config.pricePerRound(),
                config.status().name(),
                config.createdAt()
        );
    }

    private RouletteValidationResult validation(RouletteActivationValidation validation) {
        return new RouletteValidationResult(
                validation.activatable(),
                validation.reasons(),
                validation.probabilityTotal(),
                validation.hasLosingOption()
        );
    }

    private ConfigResult requireConfig(Long configId) {
        return roulettePort.findConfigById(configId)
                .orElseThrow(() -> new IllegalArgumentException("roulette config not found"));
    }

    private RewardType parseRewardType(String value) {
        return value == null || value.isBlank() ? null : RewardType.valueOf(value.trim());
    }

    private ConversionMode parseConversionMode(String value) {
        return value == null || value.isBlank() ? null : ConversionMode.valueOf(value.trim());
    }
}
