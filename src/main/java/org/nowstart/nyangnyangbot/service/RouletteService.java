package org.nowstart.nyangnyangbot.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.data.dto.chzzk.DonationDto;
import org.nowstart.nyangnyangbot.data.dto.roulette.RouletteRunDto;
import org.nowstart.nyangnyangbot.data.dto.roulette.RouletteTableDto;
import org.nowstart.nyangnyangbot.data.entity.RouletteEventEntity;
import org.nowstart.nyangnyangbot.data.entity.RouletteItemEntity;
import org.nowstart.nyangnyangbot.data.entity.RouletteRoundResultEntity;
import org.nowstart.nyangnyangbot.data.entity.RouletteTableEntity;
import org.nowstart.nyangnyangbot.data.type.ConversionMode;
import org.nowstart.nyangnyangbot.data.type.RewardType;
import org.nowstart.nyangnyangbot.data.type.RouletteEventStatus;
import org.nowstart.nyangnyangbot.data.type.RouletteRoundStatus;
import org.nowstart.nyangnyangbot.repository.RouletteEventRepository;
import org.nowstart.nyangnyangbot.repository.RouletteItemRepository;
import org.nowstart.nyangnyangbot.repository.RouletteRoundResultRepository;
import org.nowstart.nyangnyangbot.repository.RouletteTableRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RouletteService {

    private static final int TOTAL_PROBABILITY = 10_000;
    private static final int DEFAULT_HIGH_ROUND_THRESHOLD = 100;

    private final ObjectMapper objectMapper;
    private final RouletteTableRepository rouletteTableRepository;
    private final RouletteItemRepository rouletteItemRepository;
    private final RouletteEventRepository rouletteEventRepository;
    private final RouletteRoundResultRepository rouletteRoundResultRepository;
    private final RouletteRoundApplyService rouletteRoundApplyService;

    @Transactional
    public RouletteTableDto.Response createTable(RouletteTableDto.CreateRequest request) {
        validateTableRequest(request);
        RouletteTableEntity saved = rouletteTableRepository.save(RouletteTableEntity.builder()
                .title(request.title().trim())
                .command(request.command().trim())
                .pricePerRound(request.pricePerRound())
                .active(false)
                .version(0)
                .highRoundThreshold(request.highRoundThreshold() == null
                        ? DEFAULT_HIGH_ROUND_THRESHOLD
                        : request.highRoundThreshold())
                .build());
        return tableResponse(saved);
    }

    @Transactional
    public RouletteTableDto.ItemResponse addItem(Long tableId, RouletteTableDto.ItemRequest request) {
        RouletteTableEntity table = rouletteTableRepository.findById(tableId)
                .orElseThrow(() -> new IllegalArgumentException("roulette table not found"));
        validateItemRequest(request);
        RouletteItemEntity saved = rouletteItemRepository.save(RouletteItemEntity.builder()
                .rouletteTable(table)
                .label(request.label().trim())
                .probabilityBasisPoints(request.probabilityBasisPoints())
                .losingItem(Boolean.TRUE.equals(request.losingItem()))
                .rewardType(request.rewardType())
                .conversionMode(request.conversionMode())
                .exchangeFavoriteValue(request.exchangeFavoriteValue())
                .active(true)
                .displayOrder(request.displayOrder() == null ? 0 : request.displayOrder())
                .build());
        return RouletteTableDto.ItemResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<RouletteTableDto.Response> getTables() {
        return rouletteTableRepository.findAllByOrderByIdDesc().stream()
                .map(this::tableResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public RouletteTableDto.ValidationResponse validateTable(Long tableId) {
        RouletteTableEntity table = rouletteTableRepository.findById(tableId)
                .orElseThrow(() -> new IllegalArgumentException("roulette table not found"));
        List<RouletteItemEntity> items = activeItems(tableId);
        return activationValidation(table, items);
    }

    @Transactional
    public RouletteTableDto.Response activateTable(Long tableId) {
        RouletteTableEntity table = rouletteTableRepository.findById(tableId)
                .orElseThrow(() -> new IllegalArgumentException("roulette table not found"));
        List<RouletteItemEntity> items = activeItems(tableId);
        RouletteTableDto.ValidationResponse validation = activationValidation(table, items);
        if (!validation.activatable()) {
            throw new IllegalStateException(String.join(", ", validation.reasons()));
        }
        rouletteTableRepository.findByActiveTrue().stream()
                .filter(activeTable -> !activeTable.getId().equals(table.getId()))
                .forEach(RouletteTableEntity::deactivate);
        table.activate();
        return RouletteTableDto.Response.from(rouletteTableRepository.save(table), validation, items);
    }

    @Transactional
    public RouletteTableDto.Response deactivateTable(Long tableId) {
        RouletteTableEntity table = rouletteTableRepository.findById(tableId)
                .orElseThrow(() -> new IllegalArgumentException("roulette table not found"));
        table.deactivate();
        return tableResponse(table);
    }

    public RouletteRunDto.Response processDonation(DonationDto donationDto) {
        if (donationDto == null) {
            return RouletteRunDto.Response.ignored("donation is required");
        }
        if (isBlank(donationDto.donationEventId())) {
            return RouletteRunDto.Response.ignored("donation event id is required");
        }
        RouletteTableEntity table = rouletteTableRepository.findFirstByActiveTrueOrderByIdDesc()
                .orElse(null);
        if (table == null) {
            return RouletteRunDto.Response.ignored("active roulette table not found");
        }
        if (!containsCommand(donationDto.donationText(), table.getCommand())) {
            return RouletteRunDto.Response.ignored("roulette command not found");
        }
        if (rouletteEventRepository.existsByDonationEventId(donationDto.donationEventId())) {
            return RouletteRunDto.Response.duplicate();
        }

        long amount = parseAmount(donationDto.payAmount());
        int roundCount = calculateRoundCount(amount, table.getPricePerRound());
        if (roundCount < 1) {
            return RouletteRunDto.Response.ignored("donation amount is less than roulette price");
        }
        if (roundCount > highRoundThreshold(table)) {
            log.info("action=roulette.high_round donationEventId={} roundCount={}",
                    donationDto.donationEventId(), roundCount);
        }

        List<RouletteItemEntity> items = activeItems(table.getId());
        RouletteTableDto.ValidationResponse validation = activationValidation(table, items);
        if (!validation.activatable()) {
            return RouletteRunDto.Response.ignored("active roulette table is invalid");
        }

        RouletteEventEntity event = rouletteEventRepository.save(RouletteEventEntity.builder()
                .donationEventId(donationDto.donationEventId())
                .idempotencyKey(donationDto.donationEventId())
                .userId(donationDto.donatorChannelId())
                .nickNameSnapshot(trimToEmpty(donationDto.donatorNickname()))
                .donationAmount(amount)
                .donationText(donationDto.donationText())
                .rouletteTableId(table.getId())
                .rouletteTableVersion(table.getVersion())
                .command(table.getCommand())
                .pricePerRound(table.getPricePerRound())
                .roundCount(roundCount)
                .itemsSnapshotJson(toJson(items.stream().map(RouletteTableDto.ItemResponse::from).toList()))
                .status(RouletteEventStatus.CONFIRMED)
                .build());
        List<RouletteRoundResultEntity> rounds = rouletteRoundResultRepository.saveAll(confirmRounds(event, items));
        rounds.forEach(round -> rouletteRoundApplyService.applyRound(round.getId()));
        refreshEventStatus(event.getId());
        return RouletteRunDto.Response.confirmed(event, rounds);
    }

    @Transactional(readOnly = true)
    public List<RouletteRunDto.EventResponse> getUserEvents(String userId) {
        if (isBlank(userId)) {
            throw new IllegalArgumentException("userId is required");
        }
        return rouletteEventRepository.findByUserIdOrderByCreateDateDesc(userId).stream()
                .map(event -> RouletteRunDto.EventResponse.from(
                        event,
                        rouletteRoundResultRepository.findByRouletteEventIdOrderByRoundNoAsc(event.getId())
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RouletteRunDto.RoundResponse> getRecentRounds(String userId, int limit) {
        if (isBlank(userId)) {
            throw new IllegalArgumentException("userId is required");
        }
        return rouletteRoundResultRepository.findByRouletteEventUserIdOrderByCreateDateDesc(userId).stream()
                .limit(Math.max(1, Math.min(limit, 50)))
                .map(RouletteRunDto.RoundResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public RouletteTableDto.SimulationResponse simulate(Long tableId, int iterations) {
        List<RouletteItemEntity> items = activeItems(tableId);
        int safeIterations = Math.max(1, Math.min(iterations, 10_000));
        Map<String, Integer> counts = new LinkedHashMap<>();
        items.forEach(item -> counts.put(item.getLabel(), 0));
        for (int i = 0; i < safeIterations; i++) {
            RouletteItemEntity selected = selectItem(items);
            counts.put(selected.getLabel(), counts.get(selected.getLabel()) + 1);
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
        RouletteEventEntity event = rouletteEventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("roulette event not found"));
        List<RouletteRoundResultEntity> rounds = rouletteRoundResultRepository.findByRouletteEventIdOrderByRoundNoAsc(eventId);
        long applied = rounds.stream().filter(round -> round.getStatus() == RouletteRoundStatus.APPLIED).count();
        long failed = rounds.stream().filter(round -> round.getStatus() == RouletteRoundStatus.FAILED).count();
        if (applied == rounds.size()) {
            event.updateStatus(RouletteEventStatus.APPLIED);
        } else if (failed == rounds.size()) {
            event.updateStatus(RouletteEventStatus.FAILED);
        } else if (applied > 0 || failed > 0) {
            event.updateStatus(RouletteEventStatus.PARTIALLY_APPLIED);
        } else {
            event.updateStatus(RouletteEventStatus.CONFIRMED);
        }
        rouletteEventRepository.save(event);
    }

    private RouletteTableDto.Response tableResponse(RouletteTableEntity table) {
        List<RouletteItemEntity> items = rouletteItemRepository.findByRouletteTableIdOrderByDisplayOrderAscIdAsc(table.getId());
        return RouletteTableDto.Response.from(table, activationValidation(table, activeOnly(items)), items);
    }

    private List<RouletteRoundResultEntity> confirmRounds(RouletteEventEntity event, List<RouletteItemEntity> items) {
        List<RouletteRoundResultEntity> rounds = new ArrayList<>();
        for (int roundNo = 1; roundNo <= event.getRoundCount(); roundNo++) {
            int ticket = nextTicket(TOTAL_PROBABILITY);
            RouletteItemEntity selected = selectItem(items, ticket);
            rounds.add(RouletteRoundResultEntity.builder()
                    .rouletteEvent(event)
                    .roundNo(roundNo)
                    .itemLabel(selected.getLabel())
                    .probabilityBasisPoints(selected.getProbabilityBasisPoints())
                    .losingItem(selected.isLosingItem())
                    .rewardType(selected.getRewardType())
                    .conversionMode(selected.getConversionMode())
                    .exchangeFavoriteValue(selected.getExchangeFavoriteValue())
                    .status(RouletteRoundStatus.CONFIRMED)
                    .ticket(ticket)
                    .build());
        }
        return rounds;
    }

    private RouletteItemEntity selectItem(List<RouletteItemEntity> items) {
        return selectItem(items, nextTicket(TOTAL_PROBABILITY));
    }

    private RouletteItemEntity selectItem(List<RouletteItemEntity> items, int ticket) {
        int cumulative = 0;
        for (RouletteItemEntity item : items) {
            cumulative += item.getProbabilityBasisPoints();
            if (ticket <= cumulative) {
                return item;
            }
        }
        return items.get(items.size() - 1);
    }

    int nextTicket(int totalProbability) {
        return ThreadLocalRandom.current().nextInt(1, totalProbability + 1);
    }

    private RouletteTableDto.ValidationResponse activationValidation(
            RouletteTableEntity table,
            List<RouletteItemEntity> items
    ) {
        List<String> reasons = new ArrayList<>();
        if (isBlank(table.getCommand())) {
            reasons.add("command is required");
        }
        if (table.getPricePerRound() == null || table.getPricePerRound() <= 0) {
            reasons.add("pricePerRound is required");
        }
        int probabilityTotal = items.stream()
                .map(RouletteItemEntity::getProbabilityBasisPoints)
                .filter(value -> value != null)
                .mapToInt(Integer::intValue)
                .sum();
        if (probabilityTotal != TOTAL_PROBABILITY) {
            reasons.add("probability total must be 10000");
        }
        boolean hasLosingItem = items.stream().anyMatch(item ->
                item.isLosingItem()
                        && item.getProbabilityBasisPoints() != null
                        && item.getProbabilityBasisPoints() > 0
        );
        if (!hasLosingItem) {
            reasons.add("losing item is required");
        }
        return new RouletteTableDto.ValidationResponse(
                reasons.isEmpty(),
                reasons,
                probabilityTotal,
                hasLosingItem
        );
    }

    private List<RouletteItemEntity> activeItems(Long tableId) {
        return rouletteItemRepository.findByRouletteTableIdAndActiveTrueOrderByDisplayOrderAscIdAsc(tableId);
    }

    private List<RouletteItemEntity> activeOnly(List<RouletteItemEntity> items) {
        return items.stream()
                .filter(RouletteItemEntity::isActive)
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

    private boolean containsCommand(String donationText, String command) {
        if (isBlank(donationText) || isBlank(command)) {
            return false;
        }
        return Arrays.stream(donationText.trim().split("\\s+"))
                .anyMatch(command::equals);
    }

    private int calculateRoundCount(long amount, Long pricePerRound) {
        if (pricePerRound == null || pricePerRound <= 0) {
            return 0;
        }
        long roundCount = amount / pricePerRound;
        if (roundCount > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("roundCount is too large");
        }
        return (int) roundCount;
    }

    private int highRoundThreshold(RouletteTableEntity table) {
        return table.getHighRoundThreshold() == null ? DEFAULT_HIGH_ROUND_THRESHOLD : table.getHighRoundThreshold();
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
