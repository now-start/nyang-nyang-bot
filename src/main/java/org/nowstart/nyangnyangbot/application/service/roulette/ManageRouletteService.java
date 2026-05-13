package org.nowstart.nyangnyangbot.application.service.roulette;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.application.port.in.roulette.ManageRouletteUseCase;
import org.nowstart.nyangnyangbot.application.port.in.roulette.ManageRouletteUseCase.RouletteItemResult;
import org.nowstart.nyangnyangbot.application.port.in.roulette.ManageRouletteUseCase.RouletteSimulationResult;
import org.nowstart.nyangnyangbot.application.port.in.roulette.ManageRouletteUseCase.RouletteTableResult;
import org.nowstart.nyangnyangbot.application.port.in.roulette.ManageRouletteUseCase.RouletteValidationResult;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort.ItemResult;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort.TableResult;
import org.nowstart.nyangnyangbot.domain.roulette.RouletteActivationValidation;
import org.nowstart.nyangnyangbot.domain.roulette.RoulettePolicy;
import org.nowstart.nyangnyangbot.domain.type.ConversionMode;
import org.nowstart.nyangnyangbot.domain.type.RewardType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ManageRouletteService implements ManageRouletteUseCase {

    private final RoulettePolicy roulettePolicy = new RoulettePolicy();
    private final RoulettePort roulettePort;

    @Override
    @Transactional
    public RouletteTableResult createTable(String title, String command, Long pricePerRound, Integer highRoundThreshold) {
        roulettePolicy.validateTableInput(title, command, pricePerRound);
        TableResult saved = roulettePort.createTable(
                title.trim(),
                command.trim(),
                pricePerRound,
                highRoundThreshold == null
                        ? RoulettePolicy.DEFAULT_HIGH_ROUND_THRESHOLD
                        : highRoundThreshold
        );
        return tableResult(saved);
    }

    @Override
    @Transactional
    public RouletteItemResult addItem(
            Long tableId,
            String label,
            Integer probabilityBasisPoints,
            Boolean losingItem,
            String rewardType,
            String conversionMode,
            Integer exchangeFavoriteValue,
            Integer displayOrder
    ) {
        roulettePort.findTableById(tableId)
                .orElseThrow(() -> new IllegalArgumentException("roulette table not found"));
        RewardType parsedRewardType = parseRewardType(rewardType);
        ConversionMode parsedConversionMode = parseConversionMode(conversionMode);
        roulettePolicy.validateItemInput(
                label,
                probabilityBasisPoints,
                parsedRewardType,
                parsedConversionMode,
                exchangeFavoriteValue
        );
        ItemResult item = roulettePort.addItem(
                tableId,
                label.trim(),
                probabilityBasisPoints,
                Boolean.TRUE.equals(losingItem),
                parsedRewardType,
                parsedConversionMode,
                exchangeFavoriteValue,
                displayOrder == null ? 0 : displayOrder
        );
        return itemResult(item);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RouletteTableResult> getTables() {
        return roulettePort.findTablesOrderByIdDesc().stream()
                .map(this::tableResult)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RouletteValidationResult validateTable(Long tableId) {
        TableResult table = roulettePort.findTableById(tableId)
                .orElseThrow(() -> new IllegalArgumentException("roulette table not found"));
        return activationValidation(table, activeItems(tableId));
    }

    @Override
    @Transactional
    public RouletteTableResult activateTable(Long tableId) {
        TableResult table = roulettePort.findTableById(tableId)
                .orElseThrow(() -> new IllegalArgumentException("roulette table not found"));
        List<ItemResult> items = activeItems(tableId);
        RouletteValidationResult validation = activationValidation(table, items);
        if (!validation.activatable()) {
            throw new IllegalStateException(String.join(", ", validation.reasons()));
        }
        TableResult saved = roulettePort.activateTable(tableId);
        log.info("level=AUDIT action=roulette_table.activate result=success tableId={}", saved.id());
        return tableResult(saved, items, validation);
    }

    @Override
    @Transactional
    public RouletteTableResult deactivateTable(Long tableId) {
        TableResult table = roulettePort.deactivateTable(tableId);
        log.info("level=AUDIT action=roulette_table.deactivate result=success tableId={}", table.id());
        return tableResult(table);
    }

    @Override
    @Transactional(readOnly = true)
    public RouletteSimulationResult simulate(Long tableId, int iterations) {
        List<ItemResult> items = activeItems(tableId);
        int safeIterations = roulettePolicy.safeSimulationIterations(iterations);
        Map<String, Integer> counts = new LinkedHashMap<>();
        items.forEach(item -> counts.put(item.label(), 0));
        for (int i = 0; i < safeIterations; i++) {
            ItemResult selected = roulettePolicy.selectItem(items);
            counts.put(selected.label(), counts.get(selected.label()) + 1);
        }
        List<RouletteSimulationResult.Entry> entries = counts.entrySet().stream()
                .map(entry -> new RouletteSimulationResult.Entry(
                        entry.getKey(),
                        entry.getValue(),
                        entry.getValue() / (double) safeIterations
                ))
                .toList();
        return new RouletteSimulationResult(safeIterations, entries);
    }

    private RouletteTableResult tableResult(TableResult table) {
        List<ItemResult> items = roulettePort.findItemsByTableId(table.id());
        return tableResult(table, items, activationValidation(table, activeOnly(items)));
    }

    private RouletteTableResult tableResult(
            TableResult table,
            List<ItemResult> items,
            RouletteValidationResult validation
    ) {
        return new RouletteTableResult(
                table.id(),
                table.title(),
                table.command(),
                table.pricePerRound(),
                table.active(),
                table.version(),
                table.highRoundThreshold(),
                validation,
                items.stream().map(this::itemResult).toList()
        );
    }

    private RouletteItemResult itemResult(ItemResult item) {
        return new RouletteItemResult(
                item.id(),
                item.label(),
                item.probabilityBasisPoints(),
                item.losingItem(),
                item.rewardType() == null ? null : item.rewardType().name(),
                item.conversionMode() == null ? null : item.conversionMode().name(),
                item.exchangeFavoriteValue(),
                item.active(),
                item.displayOrder()
        );
    }

    private RouletteValidationResult activationValidation(
            TableResult table,
            List<ItemResult> items
    ) {
        RouletteActivationValidation validation = roulettePolicy.validateActivation(table, items);
        return new RouletteValidationResult(
                validation.activatable(),
                validation.reasons(),
                validation.probabilityTotal(),
                validation.hasLosingItem()
        );
    }

    private List<ItemResult> activeItems(Long tableId) {
        return roulettePort.findActiveItemsByTableId(tableId);
    }

    private List<ItemResult> activeOnly(List<ItemResult> items) {
        return items.stream()
                .filter(ItemResult::active)
                .toList();
    }

    private RewardType parseRewardType(String value) {
        if (isBlank(value)) {
            return null;
        }
        return RewardType.valueOf(value.trim());
    }

    private ConversionMode parseConversionMode(String value) {
        if (isBlank(value)) {
            return null;
        }
        return ConversionMode.valueOf(value.trim());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
