package org.nowstart.nyangnyangbot.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.data.entity.DonationEntity;
import org.nowstart.nyangnyangbot.repository.DonationRepository;

@ExtendWith(MockitoExtension.class)
class DonationServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private DonationRepository donationRepository;

    @Mock
    private RouletteService rouletteService;

    @Test
    void call_ShouldPersistDonationAndRunRouletteFlow() {
        DonationService donationService = new DonationService(objectMapper, donationRepository, rouletteService);
        given(donationRepository.existsByDonationEventId("donation-1")).willReturn(false);

        donationService.call("""
                {
                  "donationEventId": "donation-1",
                  "donationType": "CHAT",
                  "channelId": "channel-1",
                  "donatorChannelId": "user-1",
                  "donatorNickname": "치즈냥",
                  "payAmount": "1,000",
                  "donationText": "!룰렛",
                  "emojis": {}
                }
                """);

        then(donationRepository).should().save(argThat(entity ->
                "donation-1".equals(entity.getDonationEventId())
                        && entity.getPayAmount().equals(1_000L)
        ));
        then(rouletteService).should().processDonation(any());
    }

    @Test
    void call_ShouldNotPersistDuplicateDonationButStillDelegateIdempotencyToRoulette() {
        DonationService donationService = new DonationService(objectMapper, donationRepository, rouletteService);
        given(donationRepository.existsByDonationEventId("donation-1")).willReturn(true);

        donationService.call("""
                {
                  "donationEventId": "donation-1",
                  "donationType": "CHAT",
                  "channelId": "channel-1",
                  "donatorChannelId": "user-1",
                  "donatorNickname": "치즈냥",
                  "payAmount": "1,000",
                  "donationText": "!룰렛"
                }
                """);

        then(donationRepository).should(never()).save(any(DonationEntity.class));
        then(rouletteService).should().processDonation(any());
    }
}
