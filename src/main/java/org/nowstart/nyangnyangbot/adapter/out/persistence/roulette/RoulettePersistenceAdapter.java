package org.nowstart.nyangnyangbot.adapter.out.persistence.roulette;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.entity.RouletteEvent;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.entity.RouletteItem;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.entity.RouletteRoundResult;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.entity.RouletteTable;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.repository.RouletteEventRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.repository.RouletteItemRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.repository.RouletteRoundResultRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.repository.RouletteTableRepository;
import org.nowstart.nyangnyangbot.adapter.out.validation.OutboundContractValidator;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort;
import org.nowstart.nyangnyangbot.config.cache.CacheNames;
import org.nowstart.nyangnyangbot.domain.type.ConversionMode;
import org.nowstart.nyangnyangbot.domain.type.RewardType;
import org.nowstart.nyangnyangbot.domain.type.RouletteEventStatus;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RoulettePersistenceAdapter implements RoulettePort {

    private final RouletteTableRepository rouletteTableRepository;
    private final RouletteItemRepository rouletteItemRepository;
    private final RouletteEventRepository rouletteEventRepository;
    private final RouletteRoundResultRepository rouletteRoundResultRepository;
    private final ObjectMapper objectMapper;
    private final OutboundContractValidator contractValidator;

    @Override
    @CacheEvict(cacheNames = CacheNames.ROULETTE_TABLES, allEntries = true)
    public TableResult createTable(String title, String command, Long pricePerRound, Integer highRoundThreshold) {
        RouletteTable saved = rouletteTableRepository.save(RouletteTable.builder()
                .title(title)
                .command(command)
                .pricePerRound(pricePerRound)
                .active(false)
                .version(0)
                .highRoundThreshold(highRoundThreshold)
                .build());
        return tableResult(saved);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.ROULETTE_ITEMS_BY_TABLE_ID, key = "#tableId"),
            @CacheEvict(cacheNames = CacheNames.ROULETTE_ACTIVE_ITEMS_BY_TABLE_ID, key = "#tableId")
    })
    public ItemResult addItem(
            Long tableId,
            String label,
            Integer probabilityBasisPoints,
            boolean losingItem,
            RewardType rewardType,
            ConversionMode conversionMode,
            Integer exchangeFavoriteValue,
            Integer displayOrder
    ) {
        RouletteTable table = rouletteTableRepository.findById(tableId)
                .orElseThrow(() -> new IllegalArgumentException("roulette table not found"));
        RouletteItem saved = rouletteItemRepository.save(RouletteItem.builder()
                .rouletteTable(table)
                .label(label)
                .probabilityBasisPoints(probabilityBasisPoints)
                .losingItem(losingItem)
                .rewardType(rewardType)
                .conversionMode(conversionMode)
                .exchangeFavoriteValue(exchangeFavoriteValue)
                .active(true)
                .displayOrder(displayOrder)
                .build());
        return itemResult(saved);
    }

    @Override
    @Cacheable(cacheNames = CacheNames.ROULETTE_TABLES)
    public List<TableResult> findTablesOrderByIdDesc() {
        return rouletteTableRepository.findAllByOrderByIdDesc().stream()
                .map(this::tableResult)
                .toList();
    }

    @Override
    @Cacheable(cacheNames = CacheNames.ROULETTE_TABLE_BY_ID, key = "#tableId", unless = "#result == null")
    public Optional<TableResult> findTableById(Long tableId) {
        return rouletteTableRepository.findById(tableId).map(this::tableResult);
    }

    @Override
    @Cacheable(cacheNames = CacheNames.ROULETTE_ITEMS_BY_TABLE_ID, key = "#tableId")
    public List<ItemResult> findItemsByTableId(Long tableId) {
        return rouletteItemRepository.findByRouletteTableIdOrderByDisplayOrderAscIdAsc(tableId).stream()
                .map(this::itemResult)
                .toList();
    }

    @Override
    @Cacheable(cacheNames = CacheNames.ROULETTE_ACTIVE_ITEMS_BY_TABLE_ID, key = "#tableId")
    public List<ItemResult> findActiveItemsByTableId(Long tableId) {
        return rouletteItemRepository.findByRouletteTableIdAndActiveTrueOrderByDisplayOrderAscIdAsc(tableId).stream()
                .map(this::itemResult)
                .toList();
    }

    @Override
    public List<TableResult> findActiveTables() {
        return rouletteTableRepository.findByActiveTrue().stream()
                .map(this::tableResult)
                .toList();
    }

    @Override
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.ROULETTE_TABLES, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.ROULETTE_TABLE_BY_ID, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.ROULETTE_LATEST_ACTIVE_TABLE, allEntries = true)
    })
    public TableResult activateTable(Long tableId) {
        RouletteTable table = rouletteTableRepository.findById(tableId)
                .orElseThrow(() -> new IllegalArgumentException("roulette table not found"));
        rouletteTableRepository.findByActiveTrue().stream()
                .filter(activeTable -> !activeTable.getId().equals(tableId))
                .findAny()
                .ifPresent(activeTable -> {
                    throw new IllegalStateException("another roulette table is already active");
        });
        table.activate();
        return tableResult(rouletteTableRepository.save(table));
    }

    @Override
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.ROULETTE_TABLES, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.ROULETTE_TABLE_BY_ID, key = "#tableId"),
            @CacheEvict(cacheNames = CacheNames.ROULETTE_LATEST_ACTIVE_TABLE, allEntries = true)
    })
    public TableResult deactivateTable(Long tableId) {
        RouletteTable table = rouletteTableRepository.findById(tableId)
                .orElseThrow(() -> new IllegalArgumentException("roulette table not found"));
        table.deactivate();
        return tableResult(table);
    }

    @Override
    @Cacheable(cacheNames = CacheNames.ROULETTE_LATEST_ACTIVE_TABLE, unless = "#result == null")
    public Optional<TableResult> findLatestActiveTable() {
        return rouletteTableRepository.findFirstByActiveTrueOrderByIdDesc().map(this::tableResult);
    }

    @Override
    public boolean existsEventByDonationEventId(String donationEventId) {
        return rouletteEventRepository.existsByDonationEventId(donationEventId);
    }

    @Override
    public EventResult createEvent(CreateRouletteEventCommand command) {
        contractValidator.request("roulette.createEvent", command);
        RouletteEvent saved = rouletteEventRepository.save(RouletteEvent.builder()
                .donationEventId(command.donationEventId())
                .idempotencyKey(command.idempotencyKey())
                .userId(command.userId())
                .nickNameSnapshot(command.nickNameSnapshot())
                .donationAmount(command.donationAmount())
                .donationText(command.donationText())
                .rouletteTableId(command.rouletteTableId())
                .rouletteTableVersion(command.rouletteTableVersion())
                .command(command.command())
                .pricePerRound(command.pricePerRound())
                .roundCount(command.roundCount())
                .itemsSnapshotJson(toJson(command.itemSnapshots()))
                .status(command.status())
                .build());
        return eventResult(saved);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("failed to serialize roulette snapshot", ex);
        }
    }

    @Override
    public List<RoundResult> saveRounds(Long rouletteEventId, List<CreateRouletteRoundCommand> commands) {
        if (commands == null) {
            contractValidator.request("roulette.saveRounds", null);
        }
        for (int index = 0; index < commands.size(); index++) {
            contractValidator.request("roulette.saveRounds[" + index + "]", commands.get(index));
        }
        RouletteEvent event = rouletteEventRepository.getReferenceById(rouletteEventId);
        List<RouletteRoundResult> rounds = commands.stream()
                .map(command -> RouletteRoundResult.builder()
                        .rouletteEvent(event)
                        .roundNo(command.roundNo())
                        .itemLabel(command.itemLabel())
                        .probabilityBasisPoints(command.probabilityBasisPoints())
                        .losingItem(command.losingItem())
                        .rewardType(command.rewardType())
                        .conversionMode(command.conversionMode())
                        .exchangeFavoriteValue(command.exchangeFavoriteValue())
                        .status(command.status())
                        .ticket(command.ticket())
                        .build())
                .toList();
        return rouletteRoundResultRepository.saveAll(rounds).stream()
                .map(this::roundResult)
                .toList();
    }

    @Override
    public List<EventResult> findEventsByUserId(String userId) {
        return rouletteEventRepository.findByUserIdOrderByCreateDateDesc(userId).stream()
                .map(this::eventResult)
                .toList();
    }

    @Override
    public Page<EventResult> findRecentEvents(Pageable pageable) {
        return rouletteEventRepository.findAllByOrderByCreateDateDesc(pageable)
                .map(this::eventResult);
    }

    @Override
    public List<RoundResult> findRoundsByEventId(Long rouletteEventId) {
        return rouletteRoundResultRepository.findByRouletteEventIdOrderByRoundNoAsc(rouletteEventId).stream()
                .map(this::roundResult)
                .toList();
    }

    @Override
    public List<RoundResult> findRoundsByUserId(String userId) {
        return rouletteRoundResultRepository.findByRouletteEventUserIdOrderByCreateDateDesc(userId).stream()
                .map(this::roundResult)
                .toList();
    }

    @Override
    public List<RoundResult> findTopRoundsByUserId(String userId, int limit) {
        return rouletteRoundResultRepository.findTop5ByRouletteEventUserIdOrderByCreateDateDesc(userId).stream()
                .limit(limit)
                .map(this::roundResult)
                .toList();
    }

    @Override
    public Optional<EventResult> findEventById(Long eventId) {
        return rouletteEventRepository.findById(eventId).map(this::eventResult);
    }

    @Override
    public void updateEventStatus(Long eventId, RouletteEventStatus status) {
        RouletteEvent event = rouletteEventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("roulette event not found"));
        event.updateStatus(status);
        rouletteEventRepository.save(event);
    }

    @Override
    public Optional<RoundResult> findRoundById(Long roundId) {
        return rouletteRoundResultRepository.findById(roundId).map(this::roundResult);
    }

    @Override
    public void markRoundApplied(Long roundId, Long ledgerId, Long userUpboId) {
        RouletteRoundResult round = rouletteRoundResultRepository.findById(roundId)
                .orElseThrow(() -> new IllegalArgumentException("roulette round not found"));
        round.markApplied(ledgerId, userUpboId);
    }

    @Override
    public void markRoundFailed(Long roundId, String failureReason) {
        RouletteRoundResult round = rouletteRoundResultRepository.findById(roundId)
                .orElseThrow(() -> new IllegalArgumentException("roulette round not found"));
        round.markFailed(failureReason);
    }

    private TableResult tableResult(RouletteTable entity) {
        return contractValidator.persistenceResult("roulette.tableResult", new TableResult(
                entity.getId(),
                entity.getTitle(),
                entity.getCommand(),
                entity.getPricePerRound(),
                entity.isActive(),
                entity.getVersion(),
                entity.getHighRoundThreshold()
        ));
    }

    private ItemResult itemResult(RouletteItem entity) {
        return contractValidator.persistenceResult("roulette.itemResult", new ItemResult(
                entity.getId(),
                entity.getRouletteTable() == null ? null : entity.getRouletteTable().getId(),
                entity.getLabel(),
                entity.getProbabilityBasisPoints(),
                entity.isLosingItem(),
                entity.getRewardType(),
                entity.getConversionMode(),
                entity.getExchangeFavoriteValue(),
                entity.isActive(),
                entity.getDisplayOrder()
        ));
    }

    private EventResult eventResult(RouletteEvent entity) {
        return contractValidator.persistenceResult("roulette.eventResult", new EventResult(
                entity.getId(),
                entity.getDonationEventId(),
                entity.getUserId(),
                entity.getNickNameSnapshot(),
                entity.getDonationAmount(),
                entity.getDonationText(),
                entity.getRouletteTableId(),
                entity.getRouletteTableVersion(),
                entity.getCommand(),
                entity.getPricePerRound(),
                entity.getRoundCount(),
                entity.getItemsSnapshotJson(),
                entity.getStatus(),
                entity.getCreateDate()
        ));
    }

    private RoundResult roundResult(RouletteRoundResult entity) {
        RouletteEvent event = entity.getRouletteEvent();
        return contractValidator.persistenceResult("roulette.roundResult", new RoundResult(
                entity.getId(),
                event == null ? null : event.getId(),
                event == null ? null : event.getDonationEventId(),
                event == null ? null : event.getUserId(),
                event == null ? null : event.getNickNameSnapshot(),
                entity.getRoundNo(),
                entity.getItemLabel(),
                entity.getProbabilityBasisPoints(),
                entity.isLosingItem(),
                entity.getRewardType(),
                entity.getConversionMode(),
                entity.getExchangeFavoriteValue(),
                entity.getStatus(),
                entity.getLedgerId(),
                entity.getUserUpboId(),
                entity.getFailureReason(),
                entity.getTicket()
        ));
    }

}
