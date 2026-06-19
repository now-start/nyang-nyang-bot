package org.nowstart.nyangnyangbot.adapter.out.persistence.roulette;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort.EventResult;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort.ItemResult;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort.RoundResult;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort.TableResult;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort.CreateRouletteEventCommand;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort.CreateRouletteRoundCommand;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.entity.RouletteEvent;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.entity.RouletteItem;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.entity.RouletteRoundResult;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.entity.RouletteTable;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.repository.RouletteEventRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.repository.RouletteItemRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.repository.RouletteRoundResultRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.repository.RouletteTableRepository;
import org.nowstart.nyangnyangbot.domain.type.ConversionMode;
import org.nowstart.nyangnyangbot.domain.type.RewardType;
import org.nowstart.nyangnyangbot.domain.type.RouletteEventStatus;
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

    @Override
    public TableResult createTable(String title, String command, Long pricePerRound, Integer highRoundThreshold) {
        RouletteTable saved = rouletteTableRepository.save(RouletteTable.builder()
                .title(title)
                .command(command)
                .pricePerRound(pricePerRound)
                .active(false)
                .version(0)
                .highRoundThreshold(highRoundThreshold)
                .build());
        return toModel(saved);
    }

    @Override
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
        return toModel(saved);
    }

    @Override
    public List<TableResult> findTablesOrderByIdDesc() {
        return rouletteTableRepository.findAllByOrderByIdDesc().stream()
                .map(this::toModel)
                .toList();
    }

    @Override
    public Optional<TableResult> findTableById(Long tableId) {
        return rouletteTableRepository.findById(tableId).map(this::toModel);
    }

    @Override
    public List<ItemResult> findItemsByTableId(Long tableId) {
        return rouletteItemRepository.findByRouletteTableIdOrderByDisplayOrderAscIdAsc(tableId).stream()
                .map(this::toModel)
                .toList();
    }

    @Override
    public List<ItemResult> findActiveItemsByTableId(Long tableId) {
        return rouletteItemRepository.findByRouletteTableIdAndActiveTrueOrderByDisplayOrderAscIdAsc(tableId).stream()
                .map(this::toModel)
                .toList();
    }

    @Override
    public TableResult activateTable(Long tableId) {
        RouletteTable table = rouletteTableRepository.findById(tableId)
                .orElseThrow(() -> new IllegalArgumentException("roulette table not found"));
        rouletteTableRepository.findByActiveTrue().stream()
                .filter(activeTable -> !activeTable.getId().equals(table.getId()))
                .forEach(RouletteTable::deactivate);
        table.activate();
        return toModel(rouletteTableRepository.save(table));
    }

    @Override
    public TableResult deactivateTable(Long tableId) {
        RouletteTable table = rouletteTableRepository.findById(tableId)
                .orElseThrow(() -> new IllegalArgumentException("roulette table not found"));
        table.deactivate();
        return toModel(table);
    }

    @Override
    public Optional<TableResult> findLatestActiveTable() {
        return rouletteTableRepository.findFirstByActiveTrueOrderByIdDesc().map(this::toModel);
    }

    @Override
    public boolean existsEventByDonationEventId(String donationEventId) {
        return rouletteEventRepository.existsByDonationEventId(donationEventId);
    }

    @Override
    public EventResult createEvent(CreateRouletteEventCommand command) {
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
                .itemsSnapshotJson(command.itemsSnapshotJson())
                .status(command.status())
                .build());
        return toModel(saved);
    }

    @Override
    public List<RoundResult> saveRounds(Long rouletteEventId, List<CreateRouletteRoundCommand> commands) {
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
                .map(this::toModel)
                .toList();
    }

    @Override
    public List<EventResult> findEventsByUserId(String userId) {
        return rouletteEventRepository.findByUserIdOrderByCreateDateDesc(userId).stream()
                .map(this::toModel)
                .toList();
    }

    @Override
    public Page<EventResult> findRecentEvents(Pageable pageable) {
        return rouletteEventRepository.findAllByOrderByCreateDateDesc(pageable)
                .map(this::toModel);
    }

    @Override
    public List<RoundResult> findRoundsByEventId(Long rouletteEventId) {
        return rouletteRoundResultRepository.findByRouletteEventIdOrderByRoundNoAsc(rouletteEventId).stream()
                .map(this::toModel)
                .toList();
    }

    @Override
    public List<RoundResult> findRoundsByUserId(String userId) {
        return rouletteRoundResultRepository.findByRouletteEventUserIdOrderByCreateDateDesc(userId).stream()
                .map(this::toModel)
                .toList();
    }

    @Override
    public List<RoundResult> findTopRoundsByUserId(String userId, int limit) {
        return rouletteRoundResultRepository.findTop5ByRouletteEventUserIdOrderByCreateDateDesc(userId).stream()
                .limit(limit)
                .map(this::toModel)
                .toList();
    }

    @Override
    public Optional<EventResult> findEventById(Long eventId) {
        return rouletteEventRepository.findById(eventId).map(this::toModel);
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
        return rouletteRoundResultRepository.findById(roundId).map(this::toModel);
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

    private TableResult toModel(RouletteTable entity) {
        return new TableResult(
                entity.getId(),
                entity.getTitle(),
                entity.getCommand(),
                entity.getPricePerRound(),
                entity.isActive(),
                entity.getVersion(),
                entity.getHighRoundThreshold()
        );
    }

    private ItemResult toModel(RouletteItem entity) {
        Long tableId = entity.getRouletteTable() == null ? null : entity.getRouletteTable().getId();
        return new ItemResult(
                entity.getId(),
                tableId,
                entity.getLabel(),
                entity.getProbabilityBasisPoints(),
                entity.isLosingItem(),
                entity.getRewardType(),
                entity.getConversionMode(),
                entity.getExchangeFavoriteValue(),
                entity.isActive(),
                entity.getDisplayOrder()
        );
    }

    private EventResult toModel(RouletteEvent entity) {
        return new EventResult(
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
        );
    }

    private RoundResult toModel(RouletteRoundResult entity) {
        RouletteEvent event = entity.getRouletteEvent();
        return new RoundResult(
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
        );
    }
}
