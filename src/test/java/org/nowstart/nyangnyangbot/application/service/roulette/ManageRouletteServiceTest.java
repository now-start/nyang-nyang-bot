package org.nowstart.nyangnyangbot.application.service.roulette;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.application.port.in.roulette.ManageRouletteUseCase.RouletteItemResult;
import org.nowstart.nyangnyangbot.application.port.in.roulette.ManageRouletteUseCase.RouletteSimulationResult;
import org.nowstart.nyangnyangbot.application.port.in.roulette.ManageRouletteUseCase.RouletteTableResult;
import org.nowstart.nyangnyangbot.application.port.in.roulette.ManageRouletteUseCase.RouletteValidationResult;
import org.nowstart.nyangnyangbot.application.port.out.command.CommandPort;
import org.nowstart.nyangnyangbot.application.port.out.command.CommandPort.CommandRecord;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort.ItemResult;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort.TableResult;
import org.nowstart.nyangnyangbot.domain.roulette.RoulettePolicy;
import org.nowstart.nyangnyangbot.domain.type.CommandActionKey;
import org.nowstart.nyangnyangbot.domain.type.CommandType;
import org.nowstart.nyangnyangbot.domain.type.ConversionMode;
import org.nowstart.nyangnyangbot.domain.type.RewardType;

@ExtendWith(MockitoExtension.class)
class ManageRouletteServiceTest {

    @Mock
    private RoulettePort roulettePort;

    @Mock
    private CommandPort commandPort;

    @Test
    void createTable_ShouldTrimInputAndUseDefaultThreshold() {
        // 준비
        ManageRouletteService service = new ManageRouletteService(roulettePort, commandPort);
        given(commandPort.findByTrigger("!룰렛")).willReturn(Optional.empty());
        given(roulettePort.createTable("기본", "!룰렛", 1_000L, RoulettePolicy.DEFAULT_HIGH_ROUND_THRESHOLD))
                .willReturn(table(1L, true, RoulettePolicy.DEFAULT_HIGH_ROUND_THRESHOLD));
        given(roulettePort.findItemsByTableId(1L)).willReturn(List.of(
                item(1L, "꽝", 10_000, true, RewardType.CUSTOM, ConversionMode.NONE, null, true)
        ));

        // 실행
        RouletteTableResult result = service.createTable(" 기본 ", " !룰렛 ", 1_000L, null);

        // 검증
        then(result.id()).isEqualTo(1L);
        then(result.highRoundThreshold()).isEqualTo(RoulettePolicy.DEFAULT_HIGH_ROUND_THRESHOLD);
        then(result.validation().activatable()).isTrue();
        BDDMockito.then(roulettePort).should()
                .createTable("기본", "!룰렛", 1_000L, RoulettePolicy.DEFAULT_HIGH_ROUND_THRESHOLD);
        BDDMockito.then(commandPort).should(never()).create(any());
        BDDMockito.then(commandPort).should(never()).update(any());
    }

    @Test
    void createTable_ShouldRejectWhenCommandConflictsWithManagedCommand() {
        // 준비
        ManageRouletteService service = new ManageRouletteService(roulettePort, commandPort);
        given(commandPort.findByTrigger("!호감도"))
                .willReturn(Optional.of(command(10L, "!호감도", CommandActionKey.FAVORITE_STATUS)));

        // 실행 및 검증
        thenThrownBy(() -> service.createTable("기본", "!호감도", 1_000L, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("roulette command conflicts with existing command");
        BDDMockito.then(roulettePort).should(never()).createTable(any(), any(), any(), any());
    }

    @Test
    void addItem_ShouldValidateTableAndParsedEnums() {
        // 준비
        ManageRouletteService service = new ManageRouletteService(roulettePort, commandPort);
        given(roulettePort.findTableById(1L)).willReturn(Optional.of(table(1L, false, 10)));
        given(roulettePort.addItem(1L, "호감도", 5_000, false, RewardType.FAVORITE, ConversionMode.AUTO, 10, 0))
                .willReturn(item(2L, "호감도", 5_000, false, RewardType.FAVORITE, ConversionMode.AUTO, 10, true));

        // 실행
        RouletteItemResult result = service.addItem(1L, " 호감도 ", 5_000, false, " FAVORITE ", " AUTO ", 10, null);

        // 검증
        then(result.id()).isEqualTo(2L);
        then(result.rewardType()).isEqualTo("FAVORITE");
        then(result.conversionMode()).isEqualTo("AUTO");
        BDDMockito.then(roulettePort).should()
                .addItem(1L, "호감도", 5_000, false, RewardType.FAVORITE, ConversionMode.AUTO, 10, 0);
    }

    @Test
    void addItem_ShouldRejectMissingTable() {
        // 준비
        ManageRouletteService service = new ManageRouletteService(roulettePort, commandPort);
        given(roulettePort.findTableById(404L)).willReturn(Optional.empty());

        // 실행 및 검증
        thenThrownBy(() -> service.addItem(404L, "꽝", 10_000, true, "CUSTOM", "NONE", null, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("roulette table not found");
    }

    @Test
    void getTables_ShouldReturnValidationFromActiveItemsOnly() {
        // 준비
        ManageRouletteService service = new ManageRouletteService(roulettePort, commandPort);
        given(roulettePort.findTablesOrderByIdDesc()).willReturn(List.of(table(1L, false, 10)));
        given(roulettePort.findItemsByTableId(1L)).willReturn(List.of(
                item(1L, "활성 꽝", 10_000, true, RewardType.CUSTOM, ConversionMode.NONE, null, true),
                item(2L, "비활성", 5_000, false, RewardType.CUSTOM, ConversionMode.NONE, null, false)
        ));

        // 실행
        List<RouletteTableResult> result = service.getTables();

        // 검증
        then(result).hasSize(1);
        then(result.getFirst().items()).hasSize(2);
        then(result.getFirst().validation().probabilityTotal()).isEqualTo(10_000);
        then(result.getFirst().validation().activatable()).isTrue();
    }

    @Test
    void validateTable_ShouldRejectMissingTableAndReturnReasonsForInvalidTable() {
        // 준비
        ManageRouletteService service = new ManageRouletteService(roulettePort, commandPort);
        given(roulettePort.findTableById(1L)).willReturn(Optional.of(table(1L, false, 10)));
        given(roulettePort.findActiveItemsByTableId(1L)).willReturn(List.of(
                item(1L, "호감도", 5_000, false, RewardType.FAVORITE, ConversionMode.MANUAL, 10, true)
        ));
        given(roulettePort.findTableById(404L)).willReturn(Optional.empty());

        // 실행
        RouletteValidationResult result = service.validateTable(1L);

        // 검증
        then(result.activatable()).isFalse();
        then(result.reasons()).contains("probability total must be 10000", "losing item is required");
        thenThrownBy(() -> service.validateTable(404L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("roulette table not found");
    }

    @Test
    void activateAndDeactivate_ShouldReturnMappedTableResult() {
        // 준비
        ManageRouletteService service = new ManageRouletteService(roulettePort, commandPort);
        List<ItemResult> activeItems = List.of(
                item(1L, "꽝", 10_000, true, RewardType.CUSTOM, ConversionMode.NONE, null, true)
        );
        given(roulettePort.findTableById(1L)).willReturn(Optional.of(table(1L, false, 10)));
        given(roulettePort.findActiveItemsByTableId(1L)).willReturn(activeItems);
        given(roulettePort.findActiveTables()).willReturn(List.of());
        given(commandPort.findByTrigger("!룰렛")).willReturn(Optional.empty());
        given(commandPort.findByActionKey(CommandActionKey.ROULETTE_DONATION))
                .willReturn(Optional.of(command(100L, "!이전룰렛", CommandActionKey.ROULETTE_DONATION)));
        given(roulettePort.activateTable(1L)).willReturn(table(1L, true, 10));
        given(roulettePort.deactivateTable(1L)).willReturn(table(1L, false, 10));
        given(roulettePort.findItemsByTableId(1L)).willReturn(activeItems);

        // 실행
        RouletteTableResult activated = service.activateTable(1L);
        RouletteTableResult deactivated = service.deactivateTable(1L);

        // 검증
        then(activated.active()).isTrue();
        then(activated.validation().activatable()).isTrue();
        then(deactivated.active()).isFalse();
        BDDMockito.then(roulettePort).should().activateTable(1L);
        BDDMockito.then(roulettePort).should().deactivateTable(1L);
        BDDMockito.then(commandPort).should().update(argThat(data ->
                data.id().equals(100L)
                        && data.trigger().equals("!룰렛")
                        && data.active()
        ));
    }

    @Test
    void activateTable_ShouldRejectWhenAnotherTableIsActive() {
        // 준비
        ManageRouletteService service = new ManageRouletteService(roulettePort, commandPort);
        List<ItemResult> activeItems = List.of(
                item(1L, "꽝", 10_000, true, RewardType.CUSTOM, ConversionMode.NONE, null, true)
        );
        given(roulettePort.findTableById(1L)).willReturn(Optional.of(table(1L, false, 10)));
        given(roulettePort.findActiveItemsByTableId(1L)).willReturn(activeItems);
        given(roulettePort.findActiveTables()).willReturn(List.of(table(2L, true, 10)));

        // 실행 및 검증
        thenThrownBy(() -> service.activateTable(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("another roulette table is already active");
        BDDMockito.then(roulettePort).should(never()).activateTable(1L);
    }

    @Test
    void simulate_ShouldClampIterationsAndCountSelectedItems() {
        // 준비
        ManageRouletteService service = new ManageRouletteService(roulettePort, commandPort);
        given(roulettePort.findActiveItemsByTableId(1L)).willReturn(List.of(
                item(1L, "꽝", 10_000, true, RewardType.CUSTOM, ConversionMode.NONE, null, true)
        ));

        // 실행
        RouletteSimulationResult result = service.simulate(1L, 0);

        // 검증
        then(result.iterations()).isEqualTo(1);
        then(result.items()).hasSize(1);
        then(result.items().getFirst().label()).isEqualTo("꽝");
        then(result.items().getFirst().count()).isEqualTo(1);
        then(result.items().getFirst().ratio()).isEqualTo(1.0);
    }

    private TableResult table(Long id, boolean active, Integer highRoundThreshold) {
        return new TableResult(id, "기본", "!룰렛", 1_000L, active, 1, highRoundThreshold);
    }

    private CommandRecord command(Long id, String trigger, CommandActionKey actionKey) {
        return new CommandRecord(
                id,
                CommandType.TRIGGER,
                trigger,
                actionKey,
                null,
                null,
                null,
                true,
                "USER",
                0,
                "system",
                "system",
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    private ItemResult item(
            Long id,
            String label,
            Integer probabilityBasisPoints,
            boolean losingItem,
            RewardType rewardType,
            ConversionMode conversionMode,
            Integer exchangeFavoriteValue,
            boolean active
    ) {
        return new ItemResult(
                id,
                1L,
                label,
                probabilityBasisPoints,
                losingItem,
                rewardType,
                conversionMode,
                exchangeFavoriteValue,
                active,
                id.intValue()
        );
    }
}
