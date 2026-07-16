package org.nowstart.nyangnyangbot.application.port.out.roulette;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.nowstart.nyangnyangbot.domain.roulette.RouletteItemSnapshot;
import org.nowstart.nyangnyangbot.domain.roulette.RoulettePolicy;
import org.nowstart.nyangnyangbot.domain.type.ConversionMode;
import org.nowstart.nyangnyangbot.domain.type.RewardType;
import org.nowstart.nyangnyangbot.domain.type.RouletteEventStatus;
import org.nowstart.nyangnyangbot.domain.type.RouletteRoundStatus;
import org.nowstart.nyangnyangbot.application.validation.outbound.OutboundResult;
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

    Optional<EventResult> findEventById(Long eventId);

    void updateEventStatus(Long eventId, RouletteEventStatus status);

    Optional<RoundResult> findRoundById(Long roundId);

    void markRoundApplied(Long roundId, Long ledgerId, Long userUpboId);

    void markRoundFailed(Long roundId, String failureReason);

    record TableResult(
            @NotNull(groups = OutboundResult.class, message = "id is required")
            @Positive(groups = OutboundResult.class, message = "id must be positive")
            Long id,
            @NotBlank(message = "title is required")
            String title,
            @NotBlank(message = "command is required")
            String command,
            @NotNull(message = "pricePerRound is required")
            @Positive(message = "pricePerRound must be positive")
            Long pricePerRound,
            boolean active,
            @NotNull(message = "version is required")
            @PositiveOrZero(message = "version must not be negative")
            Integer version,
            @NotNull(message = "highRoundThreshold is required")
            @Positive(message = "highRoundThreshold must be positive")
            Integer highRoundThreshold
    ) implements RoulettePolicy.TableCandidate {
    }

    record ItemResult(
            @NotNull(groups = OutboundResult.class, message = "id is required")
            @Positive(groups = OutboundResult.class, message = "id must be positive")
            Long id,
            @NotNull(message = "tableId is required")
            @Positive(message = "tableId must be positive")
            Long tableId,
            @NotBlank(message = "label is required")
            String label,
            @NotNull(message = "probabilityBasisPoints is required")
            @PositiveOrZero(message = "probabilityBasisPoints must not be negative")
            @Max(value = 10_000, message = "probabilityBasisPoints must not exceed 10000")
            Integer probabilityBasisPoints,
            boolean losingItem,
            RewardType rewardType,
            ConversionMode conversionMode,
            Integer exchangeFavoriteValue,
            boolean active,
            @NotNull(message = "displayOrder is required")
            @PositiveOrZero(message = "displayOrder must not be negative")
            Integer displayOrder
    ) implements RoulettePolicy.ItemCandidate {
    }

    record EventResult(
            @NotNull(groups = OutboundResult.class, message = "id is required")
            @Positive(groups = OutboundResult.class, message = "id must be positive")
            Long id,
            String donationEventId,
            String userId,
            String nickNameSnapshot,
            Long donationAmount,
            String donationText,
            @NotNull(message = "rouletteTableId is required")
            @Positive(message = "rouletteTableId must be positive")
            Long rouletteTableId,
            @NotNull(message = "rouletteTableVersion is required")
            @PositiveOrZero(message = "rouletteTableVersion must not be negative")
            Integer rouletteTableVersion,
            @NotBlank(message = "command is required")
            String command,
            @NotNull(message = "pricePerRound is required")
            @Positive(message = "pricePerRound must be positive")
            Long pricePerRound,
            @NotNull(message = "roundCount is required")
            @Positive(message = "roundCount must be positive")
            Integer roundCount,
            @NotBlank(message = "itemsSnapshotJson is required")
            String itemsSnapshotJson,
            @NotNull(message = "status is required")
            RouletteEventStatus status,
            LocalDateTime createdAt
    ) {
    }

    record RoundResult(
            @NotNull(groups = OutboundResult.class, message = "id is required")
            @Positive(groups = OutboundResult.class, message = "id must be positive")
            Long id,
            Long rouletteEventId,
            String rouletteEventDonationEventId,
            String rouletteEventUserId,
            String rouletteEventNickNameSnapshot,
            @NotNull(message = "roundNo is required")
            @Positive(message = "roundNo must be positive")
            Integer roundNo,
            @NotBlank(message = "itemLabel is required")
            String itemLabel,
            @NotNull(message = "probabilityBasisPoints is required")
            @PositiveOrZero(message = "probabilityBasisPoints must not be negative")
            @Max(value = 10_000, message = "probabilityBasisPoints must not exceed 10000")
            Integer probabilityBasisPoints,
            boolean losingItem,
            RewardType rewardType,
            ConversionMode conversionMode,
            Integer exchangeFavoriteValue,
            @NotNull(message = "status is required")
            RouletteRoundStatus status,
            Long ledgerId,
            Long userUpboId,
            String failureReason,
            @NotNull(message = "ticket is required")
            @Positive(message = "ticket must be positive")
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
            @NotNull(message = "rouletteTableId is required")
            @Positive(message = "rouletteTableId must be positive")
            Long rouletteTableId,
            @NotNull(message = "rouletteTableVersion is required")
            @PositiveOrZero(message = "rouletteTableVersion must not be negative")
            Integer rouletteTableVersion,
            @NotBlank(message = "command is required")
            String command,
            @NotNull(message = "pricePerRound is required")
            @Positive(message = "pricePerRound must be positive")
            Long pricePerRound,
            @NotNull(message = "roundCount is required")
            @Positive(message = "roundCount must be positive")
            Integer roundCount,
            @NotEmpty(message = "itemSnapshots are required")
            List<@Valid @NotNull(message = "itemSnapshot is required") RouletteItemSnapshot> itemSnapshots,
            @NotNull(message = "status is required")
            RouletteEventStatus status
    ) {
    }

    record CreateRouletteRoundCommand(
            @NotNull(message = "roundNo is required")
            @Positive(message = "roundNo must be positive")
            Integer roundNo,
            @NotBlank(message = "itemLabel is required")
            String itemLabel,
            @NotNull(message = "probabilityBasisPoints is required")
            @PositiveOrZero(message = "probabilityBasisPoints must not be negative")
            @Max(value = 10_000, message = "probabilityBasisPoints must not exceed 10000")
            Integer probabilityBasisPoints,
            boolean losingItem,
            RewardType rewardType,
            ConversionMode conversionMode,
            Integer exchangeFavoriteValue,
            @NotNull(message = "status is required")
            RouletteRoundStatus status,
            @NotNull(message = "ticket is required")
            @Positive(message = "ticket must be positive")
            Integer ticket
    ) {
    }
}
