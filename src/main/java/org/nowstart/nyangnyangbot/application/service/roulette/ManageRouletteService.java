package org.nowstart.nyangnyangbot.application.service.roulette;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.application.port.in.roulette.ManageRouletteUseCase;
import org.nowstart.nyangnyangbot.application.port.in.roulette.ManageRouletteUseCase.AddRouletteItemCommand;
import org.nowstart.nyangnyangbot.application.port.in.roulette.ManageRouletteUseCase.CreateRouletteTableCommand;
import org.nowstart.nyangnyangbot.application.port.out.command.CommandPort;
import org.nowstart.nyangnyangbot.application.port.out.command.CommandPort.CommandRecord;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort.ItemResult;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort.TableResult;
import org.nowstart.nyangnyangbot.application.validation.UseCaseValidator;
import org.nowstart.nyangnyangbot.domain.chat.CommandTrigger;
import org.nowstart.nyangnyangbot.domain.roulette.RouletteActivationValidation;
import org.nowstart.nyangnyangbot.domain.roulette.RoulettePolicy;
import org.nowstart.nyangnyangbot.domain.type.CommandActionKey;
import org.nowstart.nyangnyangbot.domain.type.CommandType;
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
    private final CommandPort commandPort;
    private final UseCaseValidator useCaseValidator;

    @Override
    @Transactional
    public RouletteTableResult createTable(CreateRouletteTableCommand request) {
        useCaseValidator.validate(request, "request is required");
        roulettePolicy.validateTableInput(request.title(), request.command(), request.pricePerRound());
        if (!roulettePort.findTablesOrderByIdDesc().isEmpty()) {
            throw new IllegalStateException("roulette table already exists");
        }
        String normalizedCommand = CommandTrigger.normalize(request.command());
        CommandTrigger.validate(normalizedCommand);
        validateRouletteCommandConflict(normalizedCommand);
        TableResult saved = roulettePort.createTable(
                request.title().trim(),
                normalizedCommand,
                request.pricePerRound(),
                request.highRoundThreshold() == null
                        ? RoulettePolicy.DEFAULT_HIGH_ROUND_THRESHOLD
                        : request.highRoundThreshold()
        );
        return tableResult(saved);
    }

    @Override
    @Transactional
    public RouletteItemResult addItem(AddRouletteItemCommand request) {
        useCaseValidator.validate(request, "request is required");
        roulettePort.findTableById(request.tableId())
                .orElseThrow(() -> new IllegalArgumentException("roulette table not found"));
        RewardType parsedRewardType = parseRewardType(request.rewardType());
        ConversionMode parsedConversionMode = parseConversionMode(request.conversionMode());
        roulettePolicy.validateItemInput(
                request.label(),
                request.probabilityBasisPoints(),
                parsedRewardType,
                parsedConversionMode,
                request.exchangeFavoriteValue()
        );
        ItemResult item = roulettePort.addItem(
                request.tableId(),
                request.label().trim(),
                request.probabilityBasisPoints(),
                Boolean.TRUE.equals(request.losingItem()),
                parsedRewardType,
                parsedConversionMode,
                request.exchangeFavoriteValue(),
                request.displayOrder() == null ? 0 : request.displayOrder()
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
        roulettePort.findActiveTables().stream()
                .filter(activeTable -> !activeTable.id().equals(tableId))
                .findAny()
                .ifPresent(activeTable -> {
                    throw new IllegalStateException("another roulette table is already active");
                });
        syncRouletteDonationCommand(table.command());
        TableResult saved = roulettePort.activateTable(tableId);
        log.info("level=AUDIT action=roulette_table.activate result=success tableId={}", saved.id());
        return tableResult(saved, items, validation);
    }

    @Override
    @Transactional
    public RouletteTableResult deactivateTable(Long tableId) {
        TableResult current = roulettePort.findTableById(tableId)
                .orElseThrow(() -> new IllegalArgumentException("roulette table not found"));
        TableResult table = roulettePort.deactivateTable(tableId);
        if (current.active()) {
            deactivateRouletteDonationCommand(table.command());
        }
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

    private void syncRouletteDonationCommand(String command) {
        String normalizedCommand = CommandTrigger.normalize(command);
        validateRouletteCommandConflict(normalizedCommand);
        CommandRecord current = commandPort.findByActionKey(CommandActionKey.ROULETTE_DONATION)
                .orElse(null);
        if (current == null) {
            commandPort.create(new CommandPort.CreateData(
                    CommandType.TRIGGER,
                    normalizedCommand,
                    CommandActionKey.ROULETTE_DONATION,
                    null,
                    null,
                    null,
                    true,
                    "USER",
                    0,
                    "system",
                    "system"
            ));
            return;
        }
        commandPort.update(new CommandPort.UpdateData(
                current.id(),
                normalizedCommand,
                current.messageTemplate(),
                current.timerIntervalMinutes(),
                current.timerMinChatCount(),
                true,
                current.requiredRole() == null ? "USER" : current.requiredRole(),
                current.userCooldownSeconds() == null ? 0 : current.userCooldownSeconds(),
                "system"
        ));
    }

    private void deactivateRouletteDonationCommand(String command) {
        String normalizedCommand = CommandTrigger.normalize(command);
        CommandRecord current = commandPort.findByActionKey(CommandActionKey.ROULETTE_DONATION)
                .orElse(null);
        if (current == null || normalizedCommand == null || !normalizedCommand.equals(current.trigger())) {
            return;
        }
        commandPort.update(new CommandPort.UpdateData(
                current.id(),
                current.trigger(),
                current.messageTemplate(),
                current.timerIntervalMinutes(),
                current.timerMinChatCount(),
                false,
                current.requiredRole() == null ? "USER" : current.requiredRole(),
                current.userCooldownSeconds() == null ? 0 : current.userCooldownSeconds(),
                "system"
        ));
    }

    private void validateRouletteCommandConflict(String command) {
        commandPort.findByTrigger(command).ifPresent(current -> {
            if (current.actionKey() != CommandActionKey.ROULETTE_DONATION) {
                throw new IllegalArgumentException("roulette command conflicts with existing command");
            }
        });
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
