package org.nowstart.nyangnyangbot.application.service.roulette;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.nowstart.nyangnyangbot.application.port.in.roulette.ManageRouletteUseCase.AddRouletteOptionCommand;
import org.nowstart.nyangnyangbot.application.port.in.roulette.ManageRouletteUseCase.CreateRouletteConfigCommand;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort.ConfigResult;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort.CreateConfigCommand;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort.CreateOptionCommand;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort.OptionResult;
import org.nowstart.nyangnyangbot.domain.type.ConversionMode;
import org.nowstart.nyangnyangbot.domain.type.RewardType;
import org.nowstart.nyangnyangbot.domain.type.RouletteConfigStatus;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

class ManageRouletteServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-23T00:00:00Z");

    @Test
    void createConfigNormalizesInputAndStartsAsDraft() {
        RoulettePort port = Mockito.mock(RoulettePort.class);
        ManageRouletteService service = service(port);
        given(port.createConfig(Mockito.any())).willReturn(config(RouletteConfigStatus.DRAFT));

        var result = service.createConfig(new CreateRouletteConfigCommand(" 기본 ", " !룰렛 ", 1_000L, null));

        then(port).should().createConfig(new CreateConfigCommand("기본", "!룰렛", 1_000L, 100, NOW));
        assertThat(result.status()).isEqualTo("DRAFT");
    }

    @Test
    void addOptionUsesPointNamingAndLongDelta() {
        RoulettePort port = Mockito.mock(RoulettePort.class);
        ManageRouletteService service = service(port);
        OptionResult option = option(1L, 7_000, false);
        given(port.addOption(Mockito.any())).willReturn(option);

        var result = service.addOption(new AddRouletteOptionCommand(
                1L, " 포인트 ", 7_000, false, "POINT", "AUTO", 100L, null
        ));

        then(port).should().addOption(new CreateOptionCommand(
                1L, "포인트", 7_000, false, RewardType.POINT, ConversionMode.AUTO, 100L, 0, NOW
        ));
        assertThat(result.pointDelta()).isEqualTo(100L);
    }

    @Test
    void activationRequiresValidImmutableOptionSet() {
        RoulettePort port = Mockito.mock(RoulettePort.class);
        ManageRouletteService service = service(port);
        given(port.findConfigById(1L)).willReturn(java.util.Optional.of(config(RouletteConfigStatus.DRAFT)));
        given(port.findOptionsByConfigId(1L)).willReturn(List.of(
                option(1L, 7_000, false),
                option(2L, 3_000, true)
        ));
        given(port.activateConfig(1L, NOW)).willReturn(config(RouletteConfigStatus.ACTIVE));

        var result = service.activateConfig(1L);

        assertThat(result.status()).isEqualTo("ACTIVE");
        then(port).should().activateConfig(1L, NOW);
    }

    @Test
    void getConfigsReturnsPagedSummariesWithoutLoadingOptions() {
        RoulettePort port = Mockito.mock(RoulettePort.class);
        ManageRouletteService service = service(port);
        PageRequest pageable = PageRequest.of(0, 20);
        given(port.findConfigs(pageable)).willReturn(new PageImpl<>(
                List.of(config(RouletteConfigStatus.DRAFT)),
                pageable,
                1
        ));

        var result = service.getConfigs(pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().getFirst().title()).isEqualTo("기본");
        then(port).should().findConfigs(pageable);
        then(port).shouldHaveNoMoreInteractions();
    }

    private ManageRouletteService service(RoulettePort port) {
        return new ManageRouletteService(port) {
            @Override
            Instant now() {
                return NOW;
            }
        };
    }

    private ConfigResult config(RouletteConfigStatus status) {
        return new ConfigResult(1L, "기본", "!룰렛", 1_000L, 100, status, NOW, NOW);
    }

    private OptionResult option(Long id, int probability, boolean losing) {
        return new OptionResult(
                id,
                1L,
                losing ? "꽝" : "포인트",
                probability,
                losing,
                losing ? RewardType.CUSTOM : RewardType.POINT,
                losing ? ConversionMode.NONE : ConversionMode.AUTO,
                losing ? null : 100L,
                id.intValue(),
                NOW
        );
    }
}
