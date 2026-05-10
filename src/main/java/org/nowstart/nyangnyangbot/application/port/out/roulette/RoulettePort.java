package org.nowstart.nyangnyangbot.application.port.out.roulette;

import java.util.List;
import java.util.Optional;
import org.nowstart.nyangnyangbot.application.model.RouletteEvent;
import org.nowstart.nyangnyangbot.application.model.RouletteItem;
import org.nowstart.nyangnyangbot.application.model.RouletteRound;
import org.nowstart.nyangnyangbot.application.model.RouletteTable;
import org.nowstart.nyangnyangbot.data.type.ConversionMode;
import org.nowstart.nyangnyangbot.data.type.RewardType;
import org.nowstart.nyangnyangbot.data.type.RouletteEventStatus;

public interface RoulettePort {

    RouletteTable createTable(String title, String command, Long pricePerRound, Integer highRoundThreshold);

    RouletteItem addItem(
            Long tableId,
            String label,
            Integer probabilityBasisPoints,
            boolean losingItem,
            RewardType rewardType,
            ConversionMode conversionMode,
            Integer exchangeFavoriteValue,
            Integer displayOrder
    );

    List<RouletteTable> findTablesOrderByIdDesc();

    Optional<RouletteTable> findTableById(Long tableId);

    List<RouletteItem> findItemsByTableId(Long tableId);

    List<RouletteItem> findActiveItemsByTableId(Long tableId);

    RouletteTable activateTable(Long tableId);

    RouletteTable deactivateTable(Long tableId);

    Optional<RouletteTable> findLatestActiveTable();

    boolean existsEventByDonationEventId(String donationEventId);

    RouletteEvent createEvent(CreateRouletteEventCommand command);

    List<RouletteRound> saveRounds(Long rouletteEventId, List<CreateRouletteRoundCommand> commands);

    List<RouletteEvent> findEventsByUserId(String userId);

    List<RouletteRound> findRoundsByEventId(Long rouletteEventId);

    List<RouletteRound> findRoundsByUserId(String userId);

    List<RouletteRound> findTopRoundsByUserId(String userId, int limit);

    Optional<RouletteEvent> findEventById(Long eventId);

    void updateEventStatus(Long eventId, RouletteEventStatus status);

    Optional<RouletteRound> findRoundById(Long roundId);

    void markRoundApplied(Long roundId, Long ledgerId, Long userUpboId);

    void markRoundFailed(Long roundId, String failureReason);
}
