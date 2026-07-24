package org.nowstart.nyangnyangbot.application.service.donation;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.nowstart.nyangnyangbot.application.port.in.chzzk.HandleChzzkEventUseCase.DonationReceived;
import org.nowstart.nyangnyangbot.application.port.in.roulette.ProcessRouletteDonationUseCase;
import org.nowstart.nyangnyangbot.application.port.in.user.ObserveUserUseCase;
import org.nowstart.nyangnyangbot.application.port.out.donation.DonationPort;
import org.nowstart.nyangnyangbot.application.port.out.donation.DonationPort.DonationResult;
import org.nowstart.nyangnyangbot.application.port.out.donation.DonationPort.SaveDonationCommand;
import org.nowstart.nyangnyangbot.application.service.roulette.RouletteRunPreparedEvent;
import org.springframework.context.ApplicationEventPublisher;

class DonationServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-23T00:00:00Z");

    @Test
    void handleObservesUsersPersistsCanonicalDonationAndStartsRun() {
        DonationPort donationPort = Mockito.mock(DonationPort.class);
        ObserveUserUseCase observeUser = Mockito.mock(ObserveUserUseCase.class);
        ProcessRouletteDonationUseCase process = Mockito.mock(ProcessRouletteDonationUseCase.class);
        ApplicationEventPublisher eventPublisher = Mockito.mock(ApplicationEventPublisher.class);
        DonationService service = new DonationService(donationPort, observeUser, process, eventPublisher) {
            @Override
            Instant now() {
                return NOW;
            }
        };
        DonationReceived donation = donation();
        given(donationPort.findByIngestionKey("event-1")).willReturn(Optional.empty());
        given(donationPort.save(Mockito.any())).willReturn(new DonationResult(
                7L, "event-1", "CHAT", "streamer-1", "viewer-1", "시청자", 10_000L, "!룰렛"
        ));
        given(process.processDonation(Mockito.eq(7L), Mockito.any()))
                .willReturn(Optional.of(7L));

        service.handle(donation);

        then(observeUser).should().observeUser("streamer-1", null);
        then(observeUser).should().observeUser("viewer-1", "시청자");
        ArgumentCaptor<SaveDonationCommand> command = ArgumentCaptor.forClass(SaveDonationCommand.class);
        then(donationPort).should().save(command.capture());
        then(process).should().processDonation(7L, new DonationReceived(
                "event-1", "CHAT", "streamer-1", "viewer-1", "시청자",
                "10000", "!룰렛", Map.of()
        ));
        then(eventPublisher).should().publishEvent(new RouletteRunPreparedEvent(7L));
        org.assertj.core.api.Assertions.assertThat(command.getValue().amount()).isEqualTo(10_000L);
        org.assertj.core.api.Assertions.assertThat(command.getValue().receivedAt()).isEqualTo(NOW);
    }

    @Test
    void duplicateEventReusesDonationInsteadOfInsertingAgain() {
        DonationPort donationPort = Mockito.mock(DonationPort.class);
        ObserveUserUseCase observeUser = Mockito.mock(ObserveUserUseCase.class);
        ProcessRouletteDonationUseCase process = Mockito.mock(ProcessRouletteDonationUseCase.class);
        ApplicationEventPublisher eventPublisher = Mockito.mock(ApplicationEventPublisher.class);
        DonationService service = new DonationService(donationPort, observeUser, process, eventPublisher);
        DonationResult existing = new DonationResult(
                7L, "event-1", "CHAT", "streamer-1", "viewer-1", "시청자", 10_000L, "!룰렛"
        );
        given(donationPort.findByIngestionKey("event-1")).willReturn(Optional.of(existing));
        given(process.processDonation(Mockito.eq(7L), Mockito.any()))
                .willReturn(Optional.of(7L));

        service.handle(donation());

        then(donationPort).should().findByIngestionKey("event-1");
        then(donationPort).shouldHaveNoMoreInteractions();
        then(process).should().processDonation(7L, new DonationReceived(
                "event-1", "CHAT", "streamer-1", "viewer-1", "시청자",
                "10000", "!룰렛", Map.of()
        ));
        then(eventPublisher).should().publishEvent(new RouletteRunPreparedEvent(7L));
    }

    private DonationReceived donation() {
        return new DonationReceived(
                "event-1",
                "CHAT",
                "streamer-1",
                "viewer-1",
                "시청자",
                "10,000 치즈",
                "!룰렛",
                Map.of()
        );
    }
}
