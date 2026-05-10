package org.nowstart.nyangnyangbot.application.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.application.gateway.out.donation.DonationPort;
import org.nowstart.nyangnyangbot.application.dto.chzzk.DonationDto;

@ExtendWith(MockitoExtension.class)
class DonationServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private DonationPort donationPort;

    @Mock
    private RouletteService rouletteService;

    @Test
    void call_ShouldPersistDonationAndRunRouletteFlow() {
        DonationService donationService = new DonationService(objectMapper, donationPort, rouletteService);
        given(donationPort.existsByDonationEventId("donation-1")).willReturn(false);

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

        then(donationPort).should().save(any(DonationDto.class), eq(1_000L), any());
        then(rouletteService).should().processDonation(any());
    }

    @Test
    void call_ShouldNotPersistDuplicateDonationButStillDelegateIdempotencyToRoulette() {
        DonationService donationService = new DonationService(objectMapper, donationPort, rouletteService);
        given(donationPort.existsByDonationEventId("donation-1")).willReturn(true);

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

        then(donationPort).should(never()).save(any(), any(), any());
        then(rouletteService).should().processDonation(any());
    }
}
