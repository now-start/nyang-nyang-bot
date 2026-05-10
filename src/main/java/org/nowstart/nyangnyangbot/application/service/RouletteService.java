package org.nowstart.nyangnyangbot.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.application.dto.chzzk.DonationDto;
import org.nowstart.nyangnyangbot.application.dto.roulette.RouletteRunDto;
import org.nowstart.nyangnyangbot.application.dto.roulette.RouletteTableDto;
import org.nowstart.nyangnyangbot.application.gateway.out.roulette.CreateRouletteEventCommand;
import org.nowstart.nyangnyangbot.application.gateway.out.roulette.CreateRouletteRoundCommand;
import org.nowstart.nyangnyangbot.application.gateway.out.roulette.RoulettePort;
import org.nowstart.nyangnyangbot.domain.model.RouletteEvent;
import org.nowstart.nyangnyangbot.domain.model.RouletteItem;
import org.nowstart.nyangnyangbot.domain.model.RouletteRound;
import org.nowstart.nyangnyangbot.domain.model.RouletteTable;
import org.nowstart.nyangnyangbot.domain.roulette.RouletteActivationValidation;
import org.nowstart.nyangnyangbot.domain.roulette.RoulettePolicy;
import org.nowstart.nyangnyangbot.domain.type.ConversionMode;
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
    public RouletteTableDto.Response createTable(RouletteTableDto.CreateRequest request) {
        validateTableRequest(request);
        RouletteTable saved = roulettePort.createTable(
                request.title().trim(),
                request.command().trim(),
                request.pricePerRound(),
                request.highRoundThreshold() == null
                        ? RoulettePolicy.DEFAULT_HIGH_ROUND_THRESHOLD
                        : request.highRoundThreshold()
        );
        return tableResponse(saved);
    }

    @Transactional
    public RouletteTableDto.ItemResponse addItem(Long tableId, RouletteTableDto.ItemRequest request) {
        roulettePort.findTableById(tableId)
                .orElseThrow(() -> new IllegalArgumentException("roulette table not found"));
        validateItemRequest(request);
        RouletteItem saved = roulettePort.addItem(
                tableId,
                request.label().trim(),
                request.probabilityBasisPoints(),
                Boolean.TRUE.equals(request.losingItem()),
                request.rewardType(),
                request.conversionMode(),
                request.exchangeFavoriteValue(),
                request.displayOrder() == null ? 0 : request.displayOrder()
        );
        return RouletteTableDto.ItemResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<RouletteTableDto.Response> getTables() {
        return roulettePort.findTablesOrderByIdDesc().stream()
                .map(this::tableResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public RouletteTableDto.ValidationResponse validateTable(Long tableId) {
        RouletteTable table = roulettePort.findTableById(tableId)
                .orElseThrow(() -> new IllegalArgumentException("roulette table not found"));
        List<RouletteItem> items = activeItems(tableId);
        return activationValidation(table, items);
    }

    @Transactional
    public RouletteTableDto.Response activateTable(Long tableId) {
        RouletteTable table = roulettePort.findTableById(tableId)
                .orElseThrow(() -> new IllegalArgumentException("roulette table not found"));
        List<RouletteItem> items = activeItems(tableId);
        RouletteTableDto.ValidationResponse validation = activationValidation(table, items);
        if (!validation.activatable()) {
            throw new IllegalStateException(String.join(", ", validation.reasons()));
        }
        RouletteTable saved = roulettePort.activateTable(tableId);
        log.info("level=AUDIT action=roulette_table.activate result=success tableId={}", saved.id());
        return RouletteTableDto.Response.from(saved, validation, items);
    }

    @Transactional
    public RouletteTableDto.Response deactivateTable(Long tableId) {
        RouletteTable table = roulettePort.deactivateTable(tableId);
        log.info("level=AUDIT action=roulette_table.deactivate result=success tableId={}", table.id());
        return tableResponse(table);
    }

    public RouletteRunDto.Response processDonation(DonationDto donationDto) {
        if (donationDto == null) {
            return RouletteRunDto.Response.ignored("donation is required");
        }
        if (isBlank(donationDto.donationEventId())) {
            return RouletteRunDto.Response.ignored("donation event id is required");
        }
        RouletteTable table = roulettePort.findLatestActiveTable()
                .orElse(null);
        if (table == null) {
            return RouletteRunDto.Response.ignored("active roulette table not found");
        }
        if (!roulettePolicy.containsCommand(donationDto.donationText(), table.command())) {
            return RouletteRunDto.Response.ignored("roulette command not found");
        }
        if (roulettePort.existsEventByDonationEventId(donationDto.donationEventId())) {
            return RouletteRunDto.Response.duplicate();
        }

        long amount = parseAmount(donationDto.payAmount());
        int roundCount = roulettePolicy.calculateRoundCount(amount, table.pricePerRound());
        if (roundCount < 1) {
            return RouletteRunDto.Response.ignored("donation amount is less than roulette price");
        }
        if (roundCount > roulettePolicy.highRoundThreshold(table)) {
            log.info("action=roulette.high_round donationEventId={} roundCount={}",
                    donationDto.donationEventId(), roundCount);
        }

        List<RouletteItem> items = activeItems(table.id());
        RouletteTableDto.ValidationResponse validation = activationValidation(table, items);
        if (!validation.activatable()) {
            return RouletteRunDto.Response.ignored("active roulette table is invalid");
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
                toJson(items.stream().map(RouletteTableDto.ItemResponse::from).toList()),
                RouletteEventStatus.CONFIRMED
        ));
        List<RouletteRound> rounds = roulettePort.saveRounds(event.id(), confirmRounds(event, items));
        rounds.forEach(round -> rouletteRoundApplyService.applyRound(round.id()));
        refreshEventStatus(event.id());
        overlayDisplayService.enqueue(event.id());
        log.info("action=roulette.run result=confirmed donationEventId={} rouletteEventId={} roundCount={}",
                donationDto.donationEventId(), event.id(), roundCount);
        return RouletteRunDto.Response.confirmed(event, rounds);
    }

    @Transactional(readOnly = true)
    public List<RouletteRunDto.EventResponse> getUserEvents(String userId) {
        if (isBlank(userId)) {
            throw new IllegalArgumentException("userId is required");
        }
        return roulettePort.findEventsByUserId(userId).stream()
                .map(event -> RouletteRunDto.EventResponse.from(
                        event,
                        roulettePort.findRoundsByEventId(event.id())
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RouletteRunDto.RoundResponse> getRecentRounds(String userId, int limit) {
        if (isBlank(userId)) {
            throw new IllegalArgumentException("userId is required");
        }
        return roulettePort.findRoundsByUserId(userId).stream()
                .limit(Math.max(1, Math.min(limit, 50)))
                .map(RouletteRunDto.RoundResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public RouletteTableDto.SimulationResponse simulate(Long tableId, int iterations) {
        List<RouletteItem> items = activeItems(tableId);
        int safeIterations = Math.max(1, Math.min(iterations, 10_000));
        Map<String, Integer> counts = new LinkedHashMap<>();
        items.forEach(item -> counts.put(item.label(), 0));
        for (int i = 0; i < safeIterations; i++) {
            RouletteItem selected = roulettePolicy.selectItem(items);
            counts.put(selected.label(), counts.get(selected.label()) + 1);
        }
        return new RouletteTableDto.SimulationResponse(
                safeIterations,
                counts.entrySet().stream()
                        .map(entry -> new RouletteTableDto.SimulationItem(
                                entry.getKey(),
                                entry.getValue(),
                                entry.getValue() / (double) safeIterations
                        ))
                        .toList()
        );
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

    private RouletteTableDto.Response tableResponse(RouletteTable table) {
        List<RouletteItem> items = roulettePort.findItemsByTableId(table.id());
        return RouletteTableDto.Response.from(table, activationValidation(table, activeOnly(items)), items);
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

    private RouletteTableDto.ValidationResponse activationValidation(
            RouletteTable table,
            List<RouletteItem> items
    ) {
        RouletteActivationValidation validation = roulettePolicy.validateActivation(table, items);
        return new RouletteTableDto.ValidationResponse(
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

    private void validateTableRequest(RouletteTableDto.CreateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }
        if (isBlank(request.title())) {
            throw new IllegalArgumentException("title is required");
        }
        if (isBlank(request.command())) {
            throw new IllegalArgumentException("command is required");
        }
        if (request.pricePerRound() == null || request.pricePerRound() <= 0) {
            throw new IllegalArgumentException("pricePerRound is required");
        }
    }

    private void validateItemRequest(RouletteTableDto.ItemRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }
        if (isBlank(request.label())) {
            throw new IllegalArgumentException("label is required");
        }
        if (request.probabilityBasisPoints() == null || request.probabilityBasisPoints() < 0) {
            throw new IllegalArgumentException("probabilityBasisPoints is required");
        }
        if (request.rewardType() == null) {
            throw new IllegalArgumentException("rewardType is required");
        }
        if (request.conversionMode() == null) {
            throw new IllegalArgumentException("conversionMode is required");
        }
        if (request.conversionMode() == ConversionMode.AUTO
                && (request.exchangeFavoriteValue() == null || request.exchangeFavoriteValue() == 0)) {
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
