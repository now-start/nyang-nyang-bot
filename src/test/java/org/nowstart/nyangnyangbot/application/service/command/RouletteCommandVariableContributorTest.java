package org.nowstart.nyangnyangbot.application.service.command;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.BDDMockito.given;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RecentRouletteResultQueryPort;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RecentRouletteResultQueryPort.RecentRound;

@ExtendWith(MockitoExtension.class)
class RouletteCommandVariableContributorTest {

    @Mock
    private RecentRouletteResultQueryPort recentRouletteResultQueryPort;

    @Test
    void resolve_ShouldReturnRecentRoundSummary() {
        // 준비
        given(recentRouletteResultQueryPort.findRecentRoundsByUserId("user-1")).willReturn(List.of(
                new RecentRound(1, "호감도 +10"),
                new RecentRound(2, "꽝")
        ));
        RouletteCommandVariableContributor contributor = new RouletteCommandVariableContributor(
                recentRouletteResultQueryPort
        );

        // 실행
        var values = contributor.resolve(Set.of("roulette.recentSummary"), context());

        // 검증
        then(values.get("roulette.recentSummary"))
                .isEqualTo("최근 룰렛 결과: 1회차 호감도 +10, 2회차 꽝");
    }

    @Test
    void resolve_ShouldReturnEmptySummaryWhenViewerHasNoRounds() {
        // 준비
        given(recentRouletteResultQueryPort.findRecentRoundsByUserId("user-1")).willReturn(List.of());
        RouletteCommandVariableContributor contributor = new RouletteCommandVariableContributor(
                recentRouletteResultQueryPort
        );

        // 실행
        var values = contributor.resolve(Set.of("roulette.recentSummary"), context());

        // 검증
        then(values.get("roulette.recentSummary")).isEqualTo("최근 룰렛 결과가 없습니다.");
    }

    @Test
    void render_ShouldKeepCompleteRecentSummaryBeyondLegacyVariableLimit() {
        // 준비
        given(recentRouletteResultQueryPort.findRecentRoundsByUserId("user-1")).willReturn(List.of(
                new RecentRound(1, "황금 치즈 조각"),
                new RecentRound(2, "호감도 보너스"),
                new RecentRound(3, "다음 방송 신청권"),
                new RecentRound(4, "냥냥 미션 쿠폰"),
                new RecentRound(5, "행운의 마지막 보상")
        ));
        CommandVariableRegistry registry = new CommandVariableRegistry(List.of(
                new RouletteCommandVariableContributor(recentRouletteResultQueryPort)
        ));
        CommandTemplateRenderer renderer = new CommandTemplateRenderer();

        // 실행
        String message = renderer.render(
                "{roulette.recentSummary}",
                registry.resolve(Set.of("roulette.recentSummary"), context())
        );

        // 검증
        then(message).hasSizeGreaterThan(60);
        then(message).endsWith("5회차 행운의 마지막 보상");
    }

    @Test
    void definitions_ShouldRegisterRouletteVariableWithoutCentralEnum() {
        // 준비
        CommandVariableRegistry registry = new CommandVariableRegistry(List.of(
                new CoreCommandVariableContributor(),
                new RouletteCommandVariableContributor(recentRouletteResultQueryPort)
        ));

        // 실행 및 검증
        then(registry.definitions())
                .extracting(CommandVariableDefinition::key)
                .contains("viewer.nickname", "roulette.recentSummary");
    }

    private CommandVariableContext context() {
        return new CommandVariableContext(
                "user-1",
                "치즈냥",
                "!룰렛결과",
                "",
                "",
                "",
                LocalDateTime.of(2026, 7, 16, 21, 0)
        );
    }
}
