package org.nowstart.nyangnyangbot.application.service.roulette;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.application.port.in.roulette.QueryRouletteResultUseCase.RouletteEventResult;
import org.nowstart.nyangnyangbot.application.port.in.roulette.QueryRouletteResultUseCase.RouletteEventSummaryResult;
import org.nowstart.nyangnyangbot.application.port.in.roulette.QueryRouletteResultUseCase.RouletteRoundResult;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort.EventResult;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort.RoundResult;
import org.nowstart.nyangnyangbot.domain.type.ConversionMode;
import org.nowstart.nyangnyangbot.domain.type.RewardType;
import org.nowstart.nyangnyangbot.domain.type.RouletteEventStatus;
import org.nowstart.nyangnyangbot.domain.type.RouletteRoundStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class QueryRouletteResultServiceTest {

    @Mock
    private RoulettePort roulettePort;

    @Test
    void getRecentRounds_ShouldLimitAndMapUserRounds() {
        // 준비
        QueryRouletteResultService service = new QueryRouletteResultService(roulettePort);
        given(roulettePort.findRoundsByUserId("user-1")).willReturn(List.of(
                round(1L, 1, "호감도 +10", false, RewardType.FAVORITE, ConversionMode.AUTO, 10, RouletteRoundStatus.APPLIED),
                round(2L, 2, "꽝", true, RewardType.CUSTOM, ConversionMode.NONE, null, RouletteRoundStatus.FAILED),
                round(3L, 3, "제외", false, null, null, null, null)
        ));

        // 실행
        List<RouletteRoundResult> result = service.getRecentRounds("user-1", 2);

        // 검증
        then(result).hasSize(2);
        then(result.getFirst().rewardType()).isEqualTo("FAVORITE");
        then(result.getFirst().conversionMode()).isEqualTo("AUTO");
        then(result.getFirst().status()).isEqualTo("APPLIED");
        then(result.get(1).losingItem()).isTrue();
        BDDMockito.then(roulettePort).should().findRoundsByUserId("user-1");
    }

    @Test
    void getRecentRounds_ShouldRejectBlankUserId() {
        // 준비
        QueryRouletteResultService service = new QueryRouletteResultService(roulettePort);

        // 실행 및 검증
        thenThrownBy(() -> service.getRecentRounds(" ", 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("userId is required");
    }

    @Test
    void getUserEvents_ShouldMapEventWithRoundsAndNullableEnums() {
        // 준비
        QueryRouletteResultService service = new QueryRouletteResultService(roulettePort);
        LocalDateTime createdAt = LocalDateTime.of(2026, 5, 16, 21, 30);
        EventResult event = new EventResult(
                10L,
                "donation-1",
                "user-1",
                "치즈냥",
                5_000L,
                "!룰렛",
                1L,
                3,
                "!룰렛",
                1_000L,
                1,
                "[]",
                null,
                createdAt
        );
        given(roulettePort.findEventsByUserId("user-1")).willReturn(List.of(event));
        given(roulettePort.findRoundsByEventId(10L)).willReturn(List.of(
                round(11L, 1, "수동 리워드", false, null, null, null, null)
        ));

        // 실행
        List<RouletteEventResult> result = service.getUserEvents("user-1");

        // 검증
        then(result).hasSize(1);
        then(result.getFirst().eventId()).isEqualTo(10L);
        then(result.getFirst().status()).isNull();
        then(result.getFirst().createdAt()).isEqualTo(createdAt);
        then(result.getFirst().rounds()).hasSize(1);
        then(result.getFirst().rounds().getFirst().rewardType()).isNull();
        BDDMockito.then(roulettePort).should().findRoundsByEventId(10L);
    }

    @Test
    void getUserEvents_ShouldRejectBlankUserId() {
        // 준비
        QueryRouletteResultService service = new QueryRouletteResultService(roulettePort);

        // 실행 및 검증
        thenThrownBy(() -> service.getUserEvents(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("userId is required");
    }

    @Test
    void getRecentEvents_ShouldMapPagedEventSummariesWithoutRounds() {
        // 준비
        QueryRouletteResultService service = new QueryRouletteResultService(roulettePort);
        PageRequest pageable = PageRequest.of(0, 5);
        LocalDateTime createdAt = LocalDateTime.of(2026, 6, 19, 15, 30);
        EventResult event = new EventResult(
                10L,
                "donation-1",
                "user-1",
                "치즈냥",
                5_000L,
                "!룰렛",
                1L,
                3,
                "!룰렛",
                1_000L,
                5,
                "[]",
                RouletteEventStatus.APPLIED,
                createdAt
        );
        given(roulettePort.findRecentEvents(pageable)).willReturn(new PageImpl<>(List.of(event), pageable, 1));
        // 실행
        Page<RouletteEventSummaryResult> result = service.getRecentEvents(pageable);

        // 검증
        then(result.getContent()).hasSize(1);
        then(result.getContent().getFirst().eventId()).isEqualTo(10L);
        then(result.getContent().getFirst().createdAt()).isEqualTo(createdAt);
        BDDMockito.then(roulettePort).should().findRecentEvents(pageable);
        BDDMockito.then(roulettePort).should(never()).findRoundsByEventId(10L);
    }

    private RoundResult round(
            Long id,
            Integer roundNo,
            String itemLabel,
            boolean losingItem,
            RewardType rewardType,
            ConversionMode conversionMode,
            Integer exchangeFavoriteValue,
            RouletteRoundStatus status
    ) {
        return new RoundResult(
                id,
                10L,
                "donation-1",
                "user-1",
                "치즈냥",
                roundNo,
                itemLabel,
                5_000,
                losingItem,
                rewardType,
                conversionMode,
                exchangeFavoriteValue,
                status,
                99L,
                100L,
                status == RouletteRoundStatus.FAILED ? "실패" : null,
                123
        );
    }
}
