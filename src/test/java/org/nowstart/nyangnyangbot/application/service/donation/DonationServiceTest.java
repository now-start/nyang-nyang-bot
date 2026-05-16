package org.nowstart.nyangnyangbot.application.service.donation;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import org.mockito.BDDMockito;
import static org.mockito.Mockito.never;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.application.port.in.roulette.ProcessRouletteDonationUseCase;
import org.nowstart.nyangnyangbot.application.port.out.donation.DonationPort;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort.DonationEventPayload;

@ExtendWith(MockitoExtension.class)
class DonationServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private DonationPort donationPort;

    @Mock
    private ProcessRouletteDonationUseCase rouletteService;

    @Test
    void call_ShouldPersistDonationAndRunRouletteFlow() {
        // 준비
        DonationService donationService = new DonationService(objectMapper, donationPort, rouletteService);
        given(donationPort.existsByDonationEventId("donation-1")).willReturn(false);

        // 실행
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

        // 검증
        BDDMockito.then(donationPort).should().save(any(DonationEventPayload.class), eq(1_000L), any());
        BDDMockito.then(rouletteService).should().processDonation(any());
    }

    @Test
    void call_ShouldPersistBlankDonationEventIdWithoutDuplicateCheck() {
        // 준비
        DonationService donationService = new DonationService(objectMapper, donationPort, rouletteService);

        // 실행
        donationService.call("""
                {
                  "donationEventId": " ",
                  "donationType": "CHAT",
                  "channelId": "channel-1",
                  "donatorChannelId": "user-1",
                  "donatorNickname": "치즈냥",
                  "payAmount": "",
                  "donationText": "!룰렛"
                }
                """);

        // 검증
        BDDMockito.then(donationPort).should(never()).existsByDonationEventId(anyString());
        BDDMockito.then(donationPort).should().save(any(DonationEventPayload.class), eq(0L), isNull());
        BDDMockito.then(rouletteService).should().processDonation(any());
    }

    @Test
    void call_ShouldFallbackInvalidAmountToZeroAndSerializeEmojis() {
        // 준비
        DonationService donationService = new DonationService(objectMapper, donationPort, rouletteService);
        given(donationPort.existsByDonationEventId("donation-2")).willReturn(false);
        ArgumentCaptor<String> emojisCaptor = ArgumentCaptor.forClass(String.class);

        // 실행
        donationService.call("""
                {
                  "donationEventId": "donation-2",
                  "donationType": "CHAT",
                  "channelId": "channel-1",
                  "donatorChannelId": "user-1",
                  "donatorNickname": "치즈냥",
                  "payAmount": "후원",
                  "donationText": "!룰렛",
                  "emojis": {"nyang": "cat"}
                }
                """);

        // 검증
        BDDMockito.then(donationPort).should().save(any(DonationEventPayload.class), eq(0L), emojisCaptor.capture());
        then(emojisCaptor.getValue()).contains("nyang", "cat");
    }

    @Test
    void call_ShouldFallbackOverflowAmountToZero() {
        // 준비
        DonationService donationService = new DonationService(objectMapper, donationPort, rouletteService);
        given(donationPort.existsByDonationEventId("donation-3")).willReturn(false);

        // 실행
        donationService.call("""
                {
                  "donationEventId": "donation-3",
                  "donationType": "CHAT",
                  "channelId": "channel-1",
                  "donatorChannelId": "user-1",
                  "donatorNickname": "치즈냥",
                  "payAmount": "999999999999999999999999원",
                  "donationText": "!룰렛"
                }
                """);

        // 검증
        BDDMockito.then(donationPort).should().save(any(DonationEventPayload.class), eq(0L), isNull());
    }

    @Test
    void call_ShouldNotPersistDuplicateDonationButStillDelegateIdempotencyToRoulette() {
        // 준비
        DonationService donationService = new DonationService(objectMapper, donationPort, rouletteService);
        given(donationPort.existsByDonationEventId("donation-1")).willReturn(true);

        // 실행
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

        // 검증
        BDDMockito.then(donationPort).should(never()).save(any(), any(), any());
        BDDMockito.then(rouletteService).should().processDonation(any());
    }
}
