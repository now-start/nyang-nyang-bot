package org.nowstart.nyangnyangbot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.data.dto.chzzk.DonationDto;
import org.nowstart.nyangnyangbot.data.dto.roulette.RouletteRunDto;
import org.nowstart.nyangnyangbot.data.entity.RouletteEventEntity;
import org.nowstart.nyangnyangbot.data.entity.RouletteItemEntity;
import org.nowstart.nyangnyangbot.data.entity.RouletteRoundResultEntity;
import org.nowstart.nyangnyangbot.data.entity.RouletteTableEntity;
import org.nowstart.nyangnyangbot.data.type.ConversionMode;
import org.nowstart.nyangnyangbot.data.type.RewardType;
import org.nowstart.nyangnyangbot.repository.RouletteEventRepository;
import org.nowstart.nyangnyangbot.repository.RouletteItemRepository;
import org.nowstart.nyangnyangbot.repository.RouletteRoundResultRepository;
import org.nowstart.nyangnyangbot.repository.RouletteTableRepository;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RouletteServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private RouletteTableRepository rouletteTableRepository;

    @Mock
    private RouletteItemRepository rouletteItemRepository;

    @Mock
    private RouletteEventRepository rouletteEventRepository;

    @Mock
    private RouletteRoundResultRepository rouletteRoundResultRepository;

    @Mock
    private RouletteRoundApplyService rouletteRoundApplyService;

    @Test
    void activateTable_ShouldRejectTableWithoutLosingItem() {
        RouletteService rouletteService = createService();
        given(rouletteTableRepository.findById(1L)).willReturn(Optional.of(table(false)));
        given(rouletteItemRepository.findByRouletteTableIdAndActiveTrueOrderByDisplayOrderAscIdAsc(1L))
                .willReturn(List.of(item("호감도 +10", 10_000, false, 10)));

        assertThatThrownBy(() -> rouletteService.activateTable(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("losing item is required");
    }

    @Test
    void processDonation_ShouldIgnoreDonationWithoutExactCommandToken() {
        RouletteService rouletteService = createService();
        given(rouletteTableRepository.findFirstByActiveTrueOrderByIdDesc()).willReturn(Optional.of(table(true)));

        RouletteRunDto.Response result = rouletteService.processDonation(new DonationDto(
                "donation-1",
                "CHAT",
                "channel-1",
                "user-1",
                "치즈냥",
                "5000",
                "안녕 !룰렛!",
                null
        ));

        assertThat(result.status()).isEqualTo("IGNORED");
        assertThat(result.reason()).isEqualTo("roulette command not found");
        then(rouletteEventRepository).should(never()).save(any());
    }

    @Test
    void processDonation_ShouldSkipDuplicateDonationEventId() {
        RouletteService rouletteService = createService();
        given(rouletteTableRepository.findFirstByActiveTrueOrderByIdDesc()).willReturn(Optional.of(table(true)));
        given(rouletteEventRepository.existsByDonationEventId("donation-1")).willReturn(true);

        RouletteRunDto.Response result = rouletteService.processDonation(new DonationDto(
                "donation-1",
                "CHAT",
                "channel-1",
                "user-1",
                "치즈냥",
                "5000",
                "안녕 !룰렛",
                null
        ));

        assertThat(result.status()).isEqualTo("DUPLICATE");
        then(rouletteRoundApplyService).should(never()).applyRound(any());
    }

    @Test
    void processDonation_ShouldConfirmRoundsAndApplyEachRound() {
        RouletteService rouletteService = createService();
        RouletteTableEntity table = table(true);
        given(rouletteTableRepository.findFirstByActiveTrueOrderByIdDesc()).willReturn(Optional.of(table));
        given(rouletteEventRepository.existsByDonationEventId("donation-1")).willReturn(false);
        given(rouletteItemRepository.findByRouletteTableIdAndActiveTrueOrderByDisplayOrderAscIdAsc(1L))
                .willReturn(List.of(
                        item("호감도 +10", 9_999, false, 10),
                        item("꽝", 1, true, null)
                ));
        RouletteEventEntity[] savedEvent = new RouletteEventEntity[1];
        @SuppressWarnings("unchecked")
        List<RouletteRoundResultEntity>[] savedRounds = new List[1];
        given(rouletteEventRepository.save(any(RouletteEventEntity.class))).willAnswer(invocation -> {
            RouletteEventEntity event = invocation.getArgument(0);
            ReflectionTestUtils.setField(event, "id", 20L);
            savedEvent[0] = event;
            return event;
        });
        given(rouletteRoundResultRepository.saveAll(any())).willAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            List<RouletteRoundResultEntity> rounds = invocation.getArgument(0);
            for (int index = 0; index < rounds.size(); index++) {
                ReflectionTestUtils.setField(rounds.get(index), "id", 30L + index);
            }
            savedRounds[0] = rounds;
            return rounds;
        });
        given(rouletteEventRepository.findById(20L)).willAnswer(invocation -> Optional.of(savedEvent[0]));
        given(rouletteRoundResultRepository.findByRouletteEventIdOrderByRoundNoAsc(20L))
                .willAnswer(invocation -> savedRounds[0]);

        RouletteRunDto.Response result = rouletteService.processDonation(new DonationDto(
                "donation-1",
                "CHAT",
                "channel-1",
                "user-1",
                "치즈냥",
                "2500",
                "안녕 !룰렛",
                null
        ));

        assertThat(result.status()).isEqualTo("CONFIRMED");
        assertThat(result.roundCount()).isEqualTo(2);
        then(rouletteRoundApplyService).should().applyRound(30L);
        then(rouletteRoundApplyService).should().applyRound(31L);
        ArgumentCaptor<RouletteEventEntity> eventCaptor = ArgumentCaptor.forClass(RouletteEventEntity.class);
        then(rouletteEventRepository).should(times(2)).save(eventCaptor.capture());
        assertThat(eventCaptor.getAllValues().getFirst().getItemsSnapshotJson()).contains("호감도 +10", "꽝");
    }

    private RouletteService createService() {
        return new RouletteService(
                objectMapper,
                rouletteTableRepository,
                rouletteItemRepository,
                rouletteEventRepository,
                rouletteRoundResultRepository,
                rouletteRoundApplyService
        );
    }

    private RouletteTableEntity table(boolean active) {
        return RouletteTableEntity.builder()
                .id(1L)
                .title("기본 룰렛")
                .command("!룰렛")
                .pricePerRound(1_000L)
                .active(active)
                .version(1)
                .highRoundThreshold(100)
                .build();
    }

    private RouletteItemEntity item(String label, int probability, boolean losingItem, Integer favoriteValue) {
        return RouletteItemEntity.builder()
                .id((long) probability)
                .label(label)
                .probabilityBasisPoints(probability)
                .losingItem(losingItem)
                .rewardType(favoriteValue == null ? RewardType.CUSTOM : RewardType.FAVORITE)
                .conversionMode(favoriteValue == null ? ConversionMode.NONE : ConversionMode.AUTO)
                .exchangeFavoriteValue(favoriteValue)
                .active(true)
                .displayOrder(probability)
                .build();
    }
}
