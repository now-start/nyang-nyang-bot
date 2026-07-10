package org.nowstart.nyangnyangbot.application.port.out.roulette;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.nowstart.nyangnyangbot.domain.roulette.RouletteItemSnapshot;
import org.nowstart.nyangnyangbot.domain.roulette.RoulettePolicy;
import org.nowstart.nyangnyangbot.domain.type.ConversionMode;
import org.nowstart.nyangnyangbot.domain.type.RewardType;
import org.nowstart.nyangnyangbot.domain.type.RouletteEventStatus;
import org.nowstart.nyangnyangbot.domain.type.RouletteRoundStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RoulettePort {

    TableResult createTable(String title, String command, Long pricePerRound, Integer highRoundThreshold);

    ItemResult addItem(
            Long tableId,
            String label,
            Integer probabilityBasisPoints,
            boolean losingItem,
            RewardType rewardType,
            ConversionMode conversionMode,
            Integer exchangeFavoriteValue,
            Integer displayOrder
    );

    List<TableResult> findTablesOrderByIdDesc();

    Optional<TableResult> findTableById(Long tableId);

    List<ItemResult> findItemsByTableId(Long tableId);

    List<ItemResult> findActiveItemsByTableId(Long tableId);

    List<TableResult> findActiveTables();

    TableResult activateTable(Long tableId);

    TableResult deactivateTable(Long tableId);

    Optional<TableResult> findLatestActiveTable();

    boolean existsEventByDonationEventId(String donationEventId);

    EventResult createEvent(CreateRouletteEventCommand command);

    List<RoundResult> saveRounds(Long rouletteEventId, List<CreateRouletteRoundCommand> commands);

    List<EventResult> findEventsByUserId(String userId);

    Page<EventResult> findRecentEvents(Pageable pageable);

    List<RoundResult> findRoundsByEventId(Long rouletteEventId);

    List<RoundResult> findRoundsByUserId(String userId);

    List<RoundResult> findTopRoundsByUserId(String userId, int limit);

    Optional<EventResult> findEventById(Long eventId);

    void updateEventStatus(Long eventId, RouletteEventStatus status);

    Optional<RoundResult> findRoundById(Long roundId);

    void markRoundApplied(Long roundId, Long ledgerId, Long userUpboId);

    void markRoundFailed(Long roundId, String failureReason);

    record TableResult(
            Long id,
            String title,
            String command,
            Long pricePerRound,
            boolean active,
            Integer version,
            Integer highRoundThreshold
    ) implements RoulettePolicy.TableCandidate {
    }

    record ItemResult(
            Long id,
            Long tableId,
            String label,
            Integer probabilityBasisPoints,
            boolean losingItem,
            RewardType rewardType,
            ConversionMode conversionMode,
            Integer exchangeFavoriteValue,
            boolean active,
            Integer displayOrder
    ) implements RoulettePolicy.ItemCandidate {
    }

    record EventResult(
            Long id,
            String donationEventId,
            String userId,
            String nickNameSnapshot,
            Long donationAmount,
            String donationText,
            Long rouletteTableId,
            Integer rouletteTableVersion,
            String command,
            Long pricePerRound,
            Integer roundCount,
            String itemsSnapshotJson,
            RouletteEventStatus status,
            LocalDateTime createdAt
    ) {
    }

    record RoundResult(
            Long id,
            Long rouletteEventId,
            String rouletteEventDonationEventId,
            String rouletteEventUserId,
            String rouletteEventNickNameSnapshot,
            Integer roundNo,
            String itemLabel,
            Integer probabilityBasisPoints,
            boolean losingItem,
            RewardType rewardType,
            ConversionMode conversionMode,
            Integer exchangeFavoriteValue,
            RouletteRoundStatus status,
            Long ledgerId,
            Long userUpboId,
            String failureReason,
            Integer ticket
    ) implements RoulettePolicy.RoundStatusCandidate {
    }

    record CreateRouletteEventCommand(
            String donationEventId,
            String idempotencyKey,
            String userId,
            String nickNameSnapshot,
            Long donationAmount,
            String donationText,
            Long rouletteTableId,
            Integer rouletteTableVersion,
            String command,
            Long pricePerRound,
            Integer roundCount,
            List<RouletteItemSnapshot> itemSnapshots,
            RouletteEventStatus status
    ) {
    }

    record CreateRouletteRoundCommand(
            Integer roundNo,
            String itemLabel,
            Integer probabilityBasisPoints,
            boolean losingItem,
            RewardType rewardType,
            ConversionMode conversionMode,
            Integer exchangeFavoriteValue,
            RouletteRoundStatus status,
            Integer ticket
    ) {
    }
}
