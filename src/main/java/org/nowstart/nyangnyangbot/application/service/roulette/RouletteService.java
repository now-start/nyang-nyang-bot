package org.nowstart.nyangnyangbot.application.service.roulette;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.application.port.in.roulette.dto.RouletteEventDetail;
import org.nowstart.nyangnyangbot.application.port.in.roulette.dto.RouletteRunResult;
import org.nowstart.nyangnyangbot.application.port.in.roulette.dto.RouletteSimulationResult;
import org.nowstart.nyangnyangbot.application.port.in.roulette.dto.RouletteTableSnapshot;
import org.nowstart.nyangnyangbot.application.port.in.roulette.dto.RouletteValidationResult;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.dto.DonationDto;
import org.nowstart.nyangnyangbot.application.port.out.roulette.dto.CreateRouletteEventCommand;
import org.nowstart.nyangnyangbot.application.port.out.roulette.dto.CreateRouletteRoundCommand;
import org.nowstart.nyangnyangbot.application.port.out.roulette.repository.RoulettePort;
import org.nowstart.nyangnyangbot.application.service.overlay.OverlayDisplayService;
import org.nowstart.nyangnyangbot.domain.model.RouletteEvent;
import org.nowstart.nyangnyangbot.domain.model.RouletteItem;
import org.nowstart.nyangnyangbot.domain.model.RouletteRound;
import org.nowstart.nyangnyangbot.domain.model.RouletteTable;
import org.nowstart.nyangnyangbot.domain.roulette.RouletteActivationValidation;
import org.nowstart.nyangnyangbot.domain.roulette.RoulettePolicy;
import org.nowstart.nyangnyangbot.domain.type.ConversionMode;
import org.nowstart.nyangnyangbot.domain.type.RewardType;
import org.nowstart.nyangnyangbot.domain.type.RouletteEventStatus;
import org.nowstart.nyangnyangbot.domain.type.RouletteRoundStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RouletteService {

    private final RoulettePolicy roulettePolicy = new RoulettePolicy();
    private final ObjectMapper objectMapper;
    private final RoulettePort roulettePort;
    private final RouletteRoundApplyService rouletteRoundApplyService;
    private final OverlayDisplayService overlayDisplayService;

    @Transactional
    public RouletteTableSnapshot createTable(String title, String command, Long pricePerRound, Integer highRoundThreshold) {
        validateTableInput(title, command, pricePerRound);
        RouletteTable saved = roulettePort.createTable(
                title.trim(),
                command.trim(),
                pricePerRound,
                highRoundThreshold == null
                        ? RoulettePolicy.DEFAULT_HIGH_ROUND_THRESHOLD
                        : highRoundThreshold
        );
        return tableSnapshot(saved);
    }

    @Transactional
    public RouletteItem addItem(
            Long tableId,
            String label,
            Integer probabilityBasisPoints,
            Boolean losingItem,
            RewardType rewardType,
            ConversionMode conversionMode,
            Integer exchangeFavoriteValue,
            Integer displayOrder
    ) {
        roulettePort.findTableById(tableId)
                .orElseThrow(() -> new IllegalArgumentException("roulette table not found"));
        validateItemInput(label, probabilityBasisPoints, rewardType, conversionMode, exchangeFavoriteValue);
        return roulettePort.addItem(
                tableId,
                label.trim(),
                probabilityBasisPoints,
                Boolean.TRUE.equals(losingItem),
                rewardType,
                conversionMode,
                exchangeFavoriteValue,
                displayOrder == null ? 0 : displayOrder
        );
    }

    @Transactional(readOnly = true)
    public List<RouletteTableSnapshot> getTables() {
        return roulettePort.findTablesOrderByIdDesc().stream()
                .map(this::tableSnapshot)
                .toList();
    }

    @Transactional(readOnly = true)
    public RouletteValidationResult validateTable(Long tableId) {
        RouletteTable table = roulettePort.findTableById(tableId)
                .orElseThrow(() -> new IllegalArgumentException("roulette table not found"));
        List<RouletteItem> items = activeItems(tableId);
        return activationValidation(table, items);
    }

    @Transactional
    public RouletteTableSnapshot activateTable(Long tableId) {
        RouletteTable table = roulettePort.findTableById(tableId)
                .orElseThrow(() -> new IllegalArgumentException("roulette table not found"));
        List<RouletteItem> items = activeItems(tableId);
        RouletteValidationResult validation = activationValidation(table, items);
        if (!validation.activatable()) {
            throw new IllegalStateException(String.join(", ", validation.reasons()));
        }
        RouletteTable saved = roulettePort.activateTable(tableId);
        log.info("level=AUDIT action=roulette_table.activate result=success tableId={}", saved.id());
        return new RouletteTableSnapshot(saved, items, validation);
    }

    @Transactional
    public RouletteTableSnapshot deactivateTable(Long tableId) {
        RouletteTable table = roulettePort.deactivateTable(tableId);
        log.info("level=AUDIT action=roulette_table.deactivate result=success tableId={}", table.id());
        return tableSnapshot(table);
    }

    public RouletteRunResult processDonation(DonationDto donationDto) {
        if (donationDto == null) {
            return RouletteRunResult.ignored("donation is required");
        }
        if (isBlank(donationDto.donationEventId())) {
            return RouletteRunResult.ignored("donation event id is required");
        }
        RouletteTable table = roulettePort.findLatestActiveTable()
                .orElse(null);
        if (table == null) {
            return RouletteRunResult.ignored("active roulette table not found");
        }
        if (!roulettePolicy.containsCommand(donationDto.donationText(), table.command())) {
            return RouletteRunResult.ignored("roulette command not found");
        }
        if (roulettePort.existsEventByDonationEventId(donationDto.donationEventId())) {
            return RouletteRunResult.duplicate();
        }

        long amount = parseAmount(donationDto.payAmount());
        int roundCount = roulettePolicy.calculateRoundCount(amount, table.pricePerRound());
        if (roundCount < 1) {
            return RouletteRunResult.ignored("donation amount is less than roulette price");
        }
        if (roundCount > roulettePolicy.highRoundThreshold(table)) {
            log.info("action=roulette.high_round donationEventId={} roundCount={}",
                    donationDto.donationEventId(), roundCount);
        }

        List<RouletteItem> items = activeItems(table.id());
        RouletteValidationResult validation = activationValidation(table, items);
        if (!validation.activatable()) {
            return RouletteRunResult.ignored("active roulette table is invalid");
        }

        RouletteEvent event = roulettePort.createEvent(new CreateRouletteEventCommand(
                donationDto.donationEventId(),
                donationDto.donationEventId(),
                donationDto.donatorChannelId(),
                trimToEmpty(donationDto.donatorNickname()),
                amount,
                donationDto.donationText(),
                table.id(),
                table.version(),
                table.command(),
                table.pricePerRound(),
                roundCount,
                toJson(items.stream().map(this::itemSnapshot).toList()),
                RouletteEventStatus.CONFIRMED
        ));
        List<RouletteRound> rounds = roulettePort.saveRounds(event.id(), confirmRounds(event, items));
        rounds.forEach(round -> rouletteRoundApplyService.applyRound(round.id()));
        refreshEventStatus(event.id());
        overlayDisplayService.enqueue(event.id());
        log.info("action=roulette.run result=confirmed donationEventId={} rouletteEventId={} roundCount={}",
                donationDto.donationEventId(), event.id(), roundCount);
        return RouletteRunResult.confirmed(event, rounds);
    }

    @Transactional(readOnly = true)
    public List<RouletteEventDetail> getUserEvents(String userId) {
        if (isBlank(userId)) {
            throw new IllegalArgumentException("userId is required");
        }
        return roulettePort.findEventsByUserId(userId).stream()
                .map(event -> new RouletteEventDetail(event, roulettePort.findRoundsByEventId(event.id())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RouletteRound> getRecentRounds(String userId, int limit) {
        if (isBlank(userId)) {
            throw new IllegalArgumentException("userId is required");
        }
        return roulettePort.findRoundsByUserId(userId).stream()
                .limit(Math.max(1, Math.min(limit, 50)))
                .toList();
    }

    @Transactional(readOnly = true)
    public RouletteSimulationResult simulate(Long tableId, int iterations) {
        List<RouletteItem> items = activeItems(tableId);
        int safeIterations = Math.max(1, Math.min(iterations, 10_000));
        Map<String, Integer> counts = new LinkedHashMap<>();
        items.forEach(item -> counts.put(item.label(), 0));
        for (int i = 0; i < safeIterations; i++) {
            RouletteItem selected = roulettePolicy.selectItem(items);
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

    @Transactional
    public void refreshEventStatus(Long eventId) {
        roulettePort.findEventById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("roulette event not found"));
        List<RouletteRound> rounds = roulettePort.findRoundsByEventId(eventId);
        long applied = rounds.stream().filter(round -> round.status() == RouletteRoundStatus.APPLIED).count();
        long failed = rounds.stream().filter(round -> round.status() == RouletteRoundStatus.FAILED).count();
        RouletteEventStatus status;
        if (applied == rounds.size()) {
            status = RouletteEventStatus.APPLIED;
        } else if (failed == rounds.size()) {
            status = RouletteEventStatus.FAILED;
        } else if (applied > 0 || failed > 0) {
            status = RouletteEventStatus.PARTIALLY_APPLIED;
        } else {
            status = RouletteEventStatus.CONFIRMED;
        }
        roulettePort.updateEventStatus(eventId, status);
    }

    private RouletteTableSnapshot tableSnapshot(RouletteTable table) {
        List<RouletteItem> items = roulettePort.findItemsByTableId(table.id());
        return new RouletteTableSnapshot(table, items, activationValidation(table, activeOnly(items)));
    }

    private record ItemSnapshot(
            Long id,
            String label,
            Integer probabilityBasisPoints,
            Boolean losingItem,
            RewardType rewardType,
            ConversionMode conversionMode,
            Integer exchangeFavoriteValue,
            Boolean active,
            Integer displayOrder
    ) {
    }

    private ItemSnapshot itemSnapshot(RouletteItem item) {
        return new ItemSnapshot(
                item.id(),
                item.label(),
                item.probabilityBasisPoints(),
                item.losingItem(),
                item.rewardType(),
                item.conversionMode(),
                item.exchangeFavoriteValue(),
                item.active(),
                item.displayOrder()
        );
    }

    private List<CreateRouletteRoundCommand> confirmRounds(RouletteEvent event, List<RouletteItem> items) {
        List<CreateRouletteRoundCommand> rounds = new ArrayList<>();
        for (int roundNo = 1; roundNo <= event.roundCount(); roundNo++) {
            int ticket = roulettePolicy.nextTicket(RoulettePolicy.TOTAL_PROBABILITY);
            RouletteItem selected = roulettePolicy.selectItem(items, ticket);
            rounds.add(new CreateRouletteRoundCommand(
                    roundNo,
                    selected.label(),
                    selected.probabilityBasisPoints(),
                    selected.losingItem(),
                    selected.rewardType(),
                    selected.conversionMode(),
                    selected.exchangeFavoriteValue(),
                    RouletteRoundStatus.CONFIRMED,
                    ticket
            ));
        }
        return rounds;
    }

    private RouletteValidationResult activationValidation(
            RouletteTable table,
            List<RouletteItem> items
    ) {
        RouletteActivationValidation validation = roulettePolicy.validateActivation(table, items);
        return new RouletteValidationResult(
                validation.activatable(),
                validation.reasons(),
                validation.probabilityTotal(),
                validation.hasLosingItem()
        );
    }

    private List<RouletteItem> activeItems(Long tableId) {
        return roulettePort.findActiveItemsByTableId(tableId);
    }

    private List<RouletteItem> activeOnly(List<RouletteItem> items) {
        return items.stream()
                .filter(RouletteItem::active)
                .toList();
    }

    private void validateTableInput(String title, String command, Long pricePerRound) {
        if (isBlank(title)) {
            throw new IllegalArgumentException("title is required");
        }
        if (isBlank(command)) {
            throw new IllegalArgumentException("command is required");
        }
        if (pricePerRound == null || pricePerRound <= 0) {
            throw new IllegalArgumentException("pricePerRound is required");
        }
    }

    private void validateItemInput(
            String label,
            Integer probabilityBasisPoints,
            RewardType rewardType,
            ConversionMode conversionMode,
            Integer exchangeFavoriteValue
    ) {
        if (isBlank(label)) {
            throw new IllegalArgumentException("label is required");
        }
        if (probabilityBasisPoints == null || probabilityBasisPoints < 0) {
            throw new IllegalArgumentException("probabilityBasisPoints is required");
        }
        if (rewardType == null) {
            throw new IllegalArgumentException("rewardType is required");
        }
        if (conversionMode == null) {
            throw new IllegalArgumentException("conversionMode is required");
        }
        if (conversionMode == ConversionMode.AUTO
                && (exchangeFavoriteValue == null || exchangeFavoriteValue == 0)) {
            throw new IllegalArgumentException("exchangeFavoriteValue is required for AUTO conversion");
        }
    }

    private long parseAmount(String amount) {
        if (isBlank(amount)) {
            return 0L;
        }
        String digits = amount.replaceAll("\\D", "");
        if (digits.isBlank()) {
            return 0L;
        }
        try {
            return Long.parseLong(digits);
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("failed to serialize roulette snapshot", ex);
        }
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
