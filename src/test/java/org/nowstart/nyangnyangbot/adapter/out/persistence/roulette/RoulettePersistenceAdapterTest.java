package org.nowstart.nyangnyangbot.adapter.out.persistence.roulette;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.entity.RouletteEvent;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.entity.RouletteItem;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.entity.RouletteRoundResult;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.entity.RouletteTable;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.repository.RouletteEventRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.repository.RouletteItemRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.repository.RouletteRoundResultRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.repository.RouletteTableRepository;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort.CreateRouletteEventCommand;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort.CreateRouletteRoundCommand;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort.EventResult;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort.ItemResult;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort.RoundResult;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort.TableResult;
import org.nowstart.nyangnyangbot.domain.type.ConversionMode;
import org.nowstart.nyangnyangbot.domain.type.RewardType;
import org.nowstart.nyangnyangbot.domain.type.RouletteEventStatus;
import org.nowstart.nyangnyangbot.domain.type.RouletteRoundStatus;

@ExtendWith(MockitoExtension.class)
class RoulettePersistenceAdapterTest {

    @Mock
    private RouletteTableRepository rouletteTableRepository;

    @Mock
    private RouletteItemRepository rouletteItemRepository;

    @Mock
    private RouletteEventRepository rouletteEventRepository;

    @Mock
    private RouletteRoundResultRepository rouletteRoundResultRepository;

    @Test
    void createTableAndAddItem_ShouldPersistEntitiesAndMapResults() {
        // 준비
        RoulettePersistenceAdapter adapter = adapter();
        RouletteTable table = table(1L, "기본", "!룰렛", 1_000L, false, 0, 100);
        RouletteItem item = item(2L, table, "꽝", 1_000, true);
        given(rouletteTableRepository.save(any(RouletteTable.class))).willReturn(table);
        given(rouletteTableRepository.findById(1L)).willReturn(Optional.of(table));
        given(rouletteItemRepository.save(any(RouletteItem.class))).willReturn(item);

        // 실행
        TableResult tableResult = adapter.createTable("기본", "!룰렛", 1_000L, 100);
        ItemResult itemResult = adapter.addItem(
                1L,
                "꽝",
                1_000,
                true,
                RewardType.CUSTOM,
                ConversionMode.NONE,
                null,
                1
        );

        // 검증
        then(tableResult.id()).isEqualTo(1L);
        then(tableResult.active()).isFalse();
        then(itemResult.id()).isEqualTo(2L);
        then(itemResult.tableId()).isEqualTo(1L);
        then(itemResult.losingItem()).isTrue();
        BDDMockito.then(rouletteTableRepository).should().save(any(RouletteTable.class));
        BDDMockito.then(rouletteItemRepository).should().save(any(RouletteItem.class));
    }

    @Test
    void addItem_ShouldRejectMissingTable() {
        // 준비
        RoulettePersistenceAdapter adapter = adapter();
        given(rouletteTableRepository.findById(404L)).willReturn(Optional.empty());

        // 실행 및 검증
        thenThrownBy(() -> adapter.addItem(
                404L,
                "꽝",
                1_000,
                true,
                RewardType.CUSTOM,
                ConversionMode.NONE,
                null,
                1
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("roulette table not found");
    }

    @Test
    void tableQueriesAndActivation_ShouldMapAndMutateTableState() {
        // 준비
        RoulettePersistenceAdapter adapter = adapter();
        RouletteTable target = table(1L, "기본", "!룰렛", 1_000L, false, 1, 100);
        RouletteTable otherActive = table(2L, "이전", "!이전", 500L, true, 3, 50);
        given(rouletteTableRepository.findAllByOrderByIdDesc()).willReturn(List.of(otherActive, target));
        given(rouletteTableRepository.findById(1L)).willReturn(Optional.of(target));
        given(rouletteTableRepository.findByActiveTrue()).willReturn(List.of());
        given(rouletteTableRepository.save(target)).willReturn(target);
        given(rouletteTableRepository.findFirstByActiveTrueOrderByIdDesc()).willReturn(Optional.of(target));

        // 실행
        List<TableResult> tables = adapter.findTablesOrderByIdDesc();
        Optional<TableResult> found = adapter.findTableById(1L);
        TableResult activated = adapter.activateTable(1L);
        TableResult deactivated = adapter.deactivateTable(1L);
        Optional<TableResult> latest = adapter.findLatestActiveTable();

        // 검증
        then(tables).hasSize(2);
        then(found).isPresent();
        then(activated.active()).isTrue();
        then(activated.version()).isEqualTo(2);
        then(deactivated.active()).isFalse();
        then(latest).isPresent();
    }

    @Test
    void activateTable_ShouldRejectWhenAnotherTableIsActive() {
        // 준비
        RoulettePersistenceAdapter adapter = adapter();
        RouletteTable target = table(1L, "기본", "!룰렛", 1_000L, false, 1, 100);
        RouletteTable otherActive = table(2L, "이전", "!이전", 500L, true, 3, 50);
        given(rouletteTableRepository.findById(1L)).willReturn(Optional.of(target));
        given(rouletteTableRepository.findByActiveTrue()).willReturn(List.of(otherActive));

        // 실행 및 검증
        thenThrownBy(() -> adapter.activateTable(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("another roulette table is already active");
    }

    @Test
    void itemAndEventQueries_ShouldMapRepositoryResults() {
        // 준비
        RoulettePersistenceAdapter adapter = adapter();
        RouletteTable table = table(1L, "기본", "!룰렛", 1_000L, true, 1, 100);
        RouletteItem item = item(2L, table, "당첨", 9_000, false);
        RouletteEvent event = event(3L, RouletteEventStatus.CONFIRMED);
        RouletteRoundResult round = round(4L, event, RouletteRoundStatus.CONFIRMED);
        given(rouletteItemRepository.findByRouletteTableIdOrderByDisplayOrderAscIdAsc(1L)).willReturn(List.of(item));
        given(rouletteItemRepository.findByRouletteTableIdAndActiveTrueOrderByDisplayOrderAscIdAsc(1L))
                .willReturn(List.of(item));
        given(rouletteEventRepository.existsByDonationEventId("donation-1")).willReturn(true);
        given(rouletteEventRepository.findByUserIdOrderByCreateDateDesc("user-1")).willReturn(List.of(event));
        given(rouletteEventRepository.findById(3L)).willReturn(Optional.of(event));
        given(rouletteRoundResultRepository.findByRouletteEventIdOrderByRoundNoAsc(3L)).willReturn(List.of(round));
        given(rouletteRoundResultRepository.findByRouletteEventUserIdOrderByCreateDateDesc("user-1"))
                .willReturn(List.of(round));
        given(rouletteRoundResultRepository.findTop5ByRouletteEventUserIdOrderByCreateDateDesc("user-1"))
                .willReturn(List.of(round));
        given(rouletteRoundResultRepository.findById(4L)).willReturn(Optional.of(round));

        // 실행
        List<ItemResult> items = adapter.findItemsByTableId(1L);
        List<ItemResult> activeItems = adapter.findActiveItemsByTableId(1L);
        boolean exists = adapter.existsEventByDonationEventId("donation-1");
        List<EventResult> events = adapter.findEventsByUserId("user-1");
        Optional<EventResult> foundEvent = adapter.findEventById(3L);
        List<RoundResult> roundsByEvent = adapter.findRoundsByEventId(3L);
        List<RoundResult> roundsByUser = adapter.findRoundsByUserId("user-1");
        List<RoundResult> topRounds = adapter.findTopRoundsByUserId("user-1", 1);
        Optional<RoundResult> foundRound = adapter.findRoundById(4L);

        // 검증
        then(items.getFirst().label()).isEqualTo("당첨");
        then(activeItems).hasSize(1);
        then(exists).isTrue();
        then(events.getFirst().id()).isEqualTo(3L);
        then(foundEvent).isPresent();
        then(roundsByEvent).hasSize(1);
        then(roundsByUser).hasSize(1);
        then(topRounds).hasSize(1);
        then(foundRound).isPresent();
    }

    @Test
    void createEventAndSaveRounds_ShouldPersistSnapshots() {
        // 준비
        RoulettePersistenceAdapter adapter = adapter();
        RouletteEvent event = event(3L, RouletteEventStatus.CONFIRMED);
        given(rouletteEventRepository.save(any(RouletteEvent.class))).willReturn(event);
        given(rouletteEventRepository.getReferenceById(3L)).willReturn(event);
        given(rouletteRoundResultRepository.saveAll(any())).willAnswer(invocation -> invocation.getArgument(0));

        // 실행
        EventResult eventResult = adapter.createEvent(new CreateRouletteEventCommand(
                "donation-1",
                "donation-1",
                "user-1",
                "치즈냥",
                1_000L,
                "!룰렛",
                1L,
                2,
                "!룰렛",
                1_000L,
                1,
                "[]",
                RouletteEventStatus.CONFIRMED
        ));
        List<RoundResult> rounds = adapter.saveRounds(3L, List.of(new CreateRouletteRoundCommand(
                1,
                "당첨",
                10_000,
                false,
                RewardType.FAVORITE,
                ConversionMode.AUTO,
                100,
                RouletteRoundStatus.CONFIRMED,
                777
        )));

        // 검증
        then(eventResult.id()).isEqualTo(3L);
        then(rounds).hasSize(1);
        then(rounds.getFirst().rouletteEventId()).isEqualTo(3L);
        then(rounds.getFirst().ticket()).isEqualTo(777);
    }

    @Test
    void statusMutations_ShouldUpdateEventAndRound() {
        // 준비
        RoulettePersistenceAdapter adapter = adapter();
        RouletteEvent event = event(3L, RouletteEventStatus.CONFIRMED);
        RouletteRoundResult round = round(4L, event, RouletteRoundStatus.CONFIRMED);
        given(rouletteEventRepository.findById(3L)).willReturn(Optional.of(event));
        given(rouletteRoundResultRepository.findById(4L)).willReturn(Optional.of(round));

        // 실행
        adapter.updateEventStatus(3L, RouletteEventStatus.APPLIED);
        adapter.markRoundApplied(4L, 10L, 20L);
        adapter.markRoundFailed(4L, "실패");

        // 검증
        then(event.getStatus()).isEqualTo(RouletteEventStatus.APPLIED);
        then(round.getStatus()).isEqualTo(RouletteRoundStatus.FAILED);
        then(round.getFailureReason()).isEqualTo("실패");
        BDDMockito.then(rouletteEventRepository).should().save(event);
    }

    @Test
    void statusMutations_ShouldRejectMissingEntities() {
        // 준비
        RoulettePersistenceAdapter adapter = adapter();
        given(rouletteEventRepository.findById(404L)).willReturn(Optional.empty());
        given(rouletteRoundResultRepository.findById(405L)).willReturn(Optional.empty());
        given(rouletteRoundResultRepository.findById(406L)).willReturn(Optional.empty());

        // 실행 및 검증
        thenThrownBy(() -> adapter.updateEventStatus(404L, RouletteEventStatus.APPLIED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("roulette event not found");
        thenThrownBy(() -> adapter.markRoundApplied(405L, 1L, 2L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("roulette round not found");
        thenThrownBy(() -> adapter.markRoundFailed(406L, "실패"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("roulette round not found");
    }

    @Test
    void saveRounds_ShouldMapRoundWithoutEvent() {
        // 준비
        RoulettePersistenceAdapter adapter = adapter();
        RouletteRoundResult round = round(4L, null, RouletteRoundStatus.CONFIRMED);
        given(rouletteRoundResultRepository.findById(4L)).willReturn(Optional.of(round));

        // 실행
        RoundResult result = adapter.findRoundById(4L).orElseThrow();

        // 검증
        then(result.rouletteEventId()).isNull();
        then(result.rouletteEventDonationEventId()).isNull();
        then(result.rouletteEventUserId()).isNull();
    }

    private RoulettePersistenceAdapter adapter() {
        return new RoulettePersistenceAdapter(
                rouletteTableRepository,
                rouletteItemRepository,
                rouletteEventRepository,
                rouletteRoundResultRepository
        );
    }

    private RouletteTable table(
            Long id,
            String title,
            String command,
            Long pricePerRound,
            boolean active,
            Integer version,
            Integer highRoundThreshold
    ) {
        return RouletteTable.builder()
                .id(id)
                .title(title)
                .command(command)
                .pricePerRound(pricePerRound)
                .active(active)
                .version(version)
                .highRoundThreshold(highRoundThreshold)
                .build();
    }

    private RouletteItem item(
            Long id,
            RouletteTable table,
            String label,
            Integer probabilityBasisPoints,
            boolean losingItem
    ) {
        return RouletteItem.builder()
                .id(id)
                .rouletteTable(table)
                .label(label)
                .probabilityBasisPoints(probabilityBasisPoints)
                .losingItem(losingItem)
                .rewardType(RewardType.FAVORITE)
                .conversionMode(ConversionMode.AUTO)
                .exchangeFavoriteValue(100)
                .active(true)
                .displayOrder(1)
                .build();
    }

    private RouletteEvent event(Long id, RouletteEventStatus status) {
        return RouletteEvent.builder()
                .id(id)
                .donationEventId("donation-1")
                .idempotencyKey("donation-1")
                .userId("user-1")
                .nickNameSnapshot("치즈냥")
                .donationAmount(1_000L)
                .donationText("!룰렛")
                .rouletteTableId(1L)
                .rouletteTableVersion(2)
                .command("!룰렛")
                .pricePerRound(1_000L)
                .roundCount(1)
                .itemsSnapshotJson("[]")
                .status(status)
                .build();
    }

    private RouletteRoundResult round(Long id, RouletteEvent event, RouletteRoundStatus status) {
        return RouletteRoundResult.builder()
                .id(id)
                .rouletteEvent(event)
                .roundNo(1)
                .itemLabel("당첨")
                .probabilityBasisPoints(10_000)
                .losingItem(false)
                .rewardType(RewardType.FAVORITE)
                .conversionMode(ConversionMode.AUTO)
                .exchangeFavoriteValue(100)
                .status(status)
                .ledgerId(10L)
                .userUpboId(20L)
                .failureReason(null)
                .ticket(777)
                .build();
    }
}
