package org.nowstart.nyangnyangbot.application.service.donation;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.application.port.in.chzzk.HandleChzzkEventUseCase.DonationReceived;
import org.nowstart.nyangnyangbot.application.port.in.roulette.ProcessRouletteDonationUseCase;
import org.nowstart.nyangnyangbot.application.port.out.donation.DonationPort;
import org.nowstart.nyangnyangbot.application.port.out.donation.DonationPort.SaveDonationCommand;

@ExtendWith(MockitoExtension.class)
class DonationServiceTest {

    @Mock
    private DonationPort donationPort;

    @Mock
    private ProcessRouletteDonationUseCase rouletteService;

    @Test
    void handle_ShouldPersistDonationAndRunRouletteFlow() {
        // 준비
        DonationService donationService = new DonationService(donationPort, rouletteService);
        given(donationPort.existsByDonationEventId("donation-1")).willReturn(false);

        // 실행
        donationService.handle(donation("donation-1", "1,000", Map.of()));

        // 검증
        ArgumentCaptor<SaveDonationCommand> captor = ArgumentCaptor.forClass(SaveDonationCommand.class);
        BDDMockito.then(donationPort).should().save(captor.capture());
        then(captor.getValue().payAmount()).isEqualTo(1_000L);
        BDDMockito.then(rouletteService).should().processDonation(any(DonationReceived.class));
    }

    @Test
    void handle_ShouldPersistBlankDonationEventIdWithoutDuplicateCheck() {
        // 준비
        DonationService donationService = new DonationService(donationPort, rouletteService);

        // 실행
        donationService.handle(donation(" ", "", null));

        // 검증
        ArgumentCaptor<SaveDonationCommand> captor = ArgumentCaptor.forClass(SaveDonationCommand.class);
        BDDMockito.then(donationPort).should(never()).existsByDonationEventId(anyString());
        BDDMockito.then(donationPort).should().save(captor.capture());
        then(captor.getValue().payAmount()).isZero();
        then(captor.getValue().emojis()).isNull();
        BDDMockito.then(rouletteService).should().processDonation(any(DonationReceived.class));
    }

    @Test
    void handle_ShouldFallbackInvalidAmountToZeroAndKeepEmojis() {
        // 준비
        DonationService donationService = new DonationService(donationPort, rouletteService);
        given(donationPort.existsByDonationEventId("donation-2")).willReturn(false);

        // 실행
        donationService.handle(donation("donation-2", "후원", Map.of("nyang", "cat")));

        // 검증
        ArgumentCaptor<SaveDonationCommand> captor = ArgumentCaptor.forClass(SaveDonationCommand.class);
        BDDMockito.then(donationPort).should().save(captor.capture());
        then(captor.getValue().payAmount()).isZero();
        then(captor.getValue().emojis()).containsEntry("nyang", "cat");
    }

    @Test
    void handle_ShouldFallbackOverflowAmountToZero() {
        // 준비
        DonationService donationService = new DonationService(donationPort, rouletteService);
        given(donationPort.existsByDonationEventId("donation-3")).willReturn(false);

        // 실행
        donationService.handle(donation("donation-3", "999999999999999999999999원", null));

        // 검증
        ArgumentCaptor<SaveDonationCommand> captor = ArgumentCaptor.forClass(SaveDonationCommand.class);
        BDDMockito.then(donationPort).should().save(captor.capture());
        then(captor.getValue().payAmount()).isZero();
    }

    @Test
    void handle_ShouldNotPersistDuplicateDonationButStillDelegateIdempotencyToRoulette() {
        // 준비
        DonationService donationService = new DonationService(donationPort, rouletteService);
        given(donationPort.existsByDonationEventId("donation-1")).willReturn(true);

        // 실행
        donationService.handle(donation("donation-1", "1,000", null));

        // 검증
        BDDMockito.then(donationPort).should(never()).save(any());
        BDDMockito.then(rouletteService).should().processDonation(any(DonationReceived.class));
    }

    private DonationReceived donation(String eventId, String amount, Map<String, String> emojis) {
        return new DonationReceived(
                eventId,
                "CHAT",
                "channel-1",
                "user-1",
                "치즈냥",
                amount,
                "!룰렛",
                emojis
        );
    }
}
