package org.nowstart.nyangnyangbot.adapter.out.persistence.roulette;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.application.model.RouletteEvent;
import org.nowstart.nyangnyangbot.application.model.RouletteItem;
import org.nowstart.nyangnyangbot.application.model.RouletteRound;
import org.nowstart.nyangnyangbot.application.model.RouletteTable;
import org.nowstart.nyangnyangbot.application.port.out.roulette.CreateRouletteEventCommand;
import org.nowstart.nyangnyangbot.application.port.out.roulette.CreateRouletteRoundCommand;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort;
import org.nowstart.nyangnyangbot.data.entity.RouletteEventEntity;
import org.nowstart.nyangnyangbot.data.entity.RouletteItemEntity;
import org.nowstart.nyangnyangbot.data.entity.RouletteRoundResultEntity;
import org.nowstart.nyangnyangbot.data.entity.RouletteTableEntity;
import org.nowstart.nyangnyangbot.data.type.ConversionMode;
import org.nowstart.nyangnyangbot.data.type.RewardType;
import org.nowstart.nyangnyangbot.data.type.RouletteEventStatus;
import org.nowstart.nyangnyangbot.repository.RouletteEventRepository;
import org.nowstart.nyangnyangbot.repository.RouletteItemRepository;
import org.nowstart.nyangnyangbot.repository.RouletteRoundResultRepository;
import org.nowstart.nyangnyangbot.repository.RouletteTableRepository;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RoulettePersistenceAdapter implements RoulettePort {

    private final RouletteTableRepository rouletteTableRepository;
    private final RouletteItemRepository rouletteItemRepository;
    private final RouletteEventRepository rouletteEventRepository;
    private final RouletteRoundResultRepository rouletteRoundResultRepository;

    @Override
    public RouletteTable createTable(String title, String command, Long pricePerRound, Integer highRoundThreshold) {
        RouletteTableEntity saved = rouletteTableRepository.save(RouletteTableEntity.builder()
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
    public RouletteItem addItem(
            Long tableId,
            String label,
            Integer probabilityBasisPoints,
            boolean losingItem,
            RewardType rewardType,
            ConversionMode conversionMode,
            Integer exchangeFavoriteValue,
            Integer displayOrder
    ) {
        RouletteTableEntity table = rouletteTableRepository.findById(tableId)
                .orElseThrow(() -> new IllegalArgumentException("roulette table not found"));
        RouletteItemEntity saved = rouletteItemRepository.save(RouletteItemEntity.builder()
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
    public List<RouletteTable> findTablesOrderByIdDesc() {
        return rouletteTableRepository.findAllByOrderByIdDesc().stream()
                .map(this::toModel)
                .toList();
    }

    @Override
    public Optional<RouletteTable> findTableById(Long tableId) {
        return rouletteTableRepository.findById(tableId).map(this::toModel);
    }

    @Override
    public List<RouletteItem> findItemsByTableId(Long tableId) {
        return rouletteItemRepository.findByRouletteTableIdOrderByDisplayOrderAscIdAsc(tableId).stream()
                .map(this::toModel)
                .toList();
    }

    @Override
    public List<RouletteItem> findActiveItemsByTableId(Long tableId) {
        return rouletteItemRepository.findByRouletteTableIdAndActiveTrueOrderByDisplayOrderAscIdAsc(tableId).stream()
                .map(this::toModel)
                .toList();
    }

    @Override
    public RouletteTable activateTable(Long tableId) {
        RouletteTableEntity table = rouletteTableRepository.findById(tableId)
                .orElseThrow(() -> new IllegalArgumentException("roulette table not found"));
        rouletteTableRepository.findByActiveTrue().stream()
                .filter(activeTable -> !activeTable.getId().equals(table.getId()))
                .forEach(RouletteTableEntity::deactivate);
        table.activate();
        return toModel(rouletteTableRepository.save(table));
    }

    @Override
    public RouletteTable deactivateTable(Long tableId) {
        RouletteTableEntity table = rouletteTableRepository.findById(tableId)
                .orElseThrow(() -> new IllegalArgumentException("roulette table not found"));
        table.deactivate();
        return toModel(table);
    }

    @Override
    public Optional<RouletteTable> findLatestActiveTable() {
        return rouletteTableRepository.findFirstByActiveTrueOrderByIdDesc().map(this::toModel);
    }

    @Override
    public boolean existsEventByDonationEventId(String donationEventId) {
        return rouletteEventRepository.existsByDonationEventId(donationEventId);
    }

    @Override
    public RouletteEvent createEvent(CreateRouletteEventCommand command) {
        RouletteEventEntity saved = rouletteEventRepository.save(RouletteEventEntity.builder()
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
    public List<RouletteRound> saveRounds(Long rouletteEventId, List<CreateRouletteRoundCommand> commands) {
        RouletteEventEntity event = rouletteEventRepository.getReferenceById(rouletteEventId);
        List<RouletteRoundResultEntity> rounds = commands.stream()
                .map(command -> RouletteRoundResultEntity.builder()
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
    public List<RouletteEvent> findEventsByUserId(String userId) {
        return rouletteEventRepository.findByUserIdOrderByCreateDateDesc(userId).stream()
                .map(this::toModel)
                .toList();
    }

    @Override
    public List<RouletteRound> findRoundsByEventId(Long rouletteEventId) {
        return rouletteRoundResultRepository.findByRouletteEventIdOrderByRoundNoAsc(rouletteEventId).stream()
                .map(this::toModel)
                .toList();
    }

    @Override
    public List<RouletteRound> findRoundsByUserId(String userId) {
        return rouletteRoundResultRepository.findByRouletteEventUserIdOrderByCreateDateDesc(userId).stream()
                .map(this::toModel)
                .toList();
    }

    @Override
    public List<RouletteRound> findTopRoundsByUserId(String userId, int limit) {
        return rouletteRoundResultRepository.findTop5ByRouletteEventUserIdOrderByCreateDateDesc(userId).stream()
                .limit(limit)
                .map(this::toModel)
                .toList();
    }

    @Override
    public Optional<RouletteEvent> findEventById(Long eventId) {
        return rouletteEventRepository.findById(eventId).map(this::toModel);
    }

    @Override
    public void updateEventStatus(Long eventId, RouletteEventStatus status) {
        RouletteEventEntity event = rouletteEventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("roulette event not found"));
        event.updateStatus(status);
        rouletteEventRepository.save(event);
    }

    @Override
    public Optional<RouletteRound> findRoundById(Long roundId) {
        return rouletteRoundResultRepository.findById(roundId).map(this::toModel);
    }

    @Override
    public void markRoundApplied(Long roundId, Long ledgerId, Long userUpboId) {
        RouletteRoundResultEntity round = rouletteRoundResultRepository.findById(roundId)
                .orElseThrow(() -> new IllegalArgumentException("roulette round not found"));
        round.markApplied(ledgerId, userUpboId);
    }

    @Override
    public void markRoundFailed(Long roundId, String failureReason) {
        RouletteRoundResultEntity round = rouletteRoundResultRepository.findById(roundId)
                .orElseThrow(() -> new IllegalArgumentException("roulette round not found"));
        round.markFailed(failureReason);
    }

    private RouletteTable toModel(RouletteTableEntity entity) {
        return new RouletteTable(
                entity.getId(),
                entity.getTitle(),
                entity.getCommand(),
                entity.getPricePerRound(),
                entity.isActive(),
                entity.getVersion(),
                entity.getHighRoundThreshold()
        );
    }

    private RouletteItem toModel(RouletteItemEntity entity) {
        Long tableId = entity.getRouletteTable() == null ? null : entity.getRouletteTable().getId();
        return new RouletteItem(
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

    private RouletteEvent toModel(RouletteEventEntity entity) {
        return new RouletteEvent(
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

    private RouletteRound toModel(RouletteRoundResultEntity entity) {
        RouletteEventEntity event = entity.getRouletteEvent();
        return new RouletteRound(
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
