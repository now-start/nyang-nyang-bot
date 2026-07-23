package org.nowstart.nyangnyangbot.application.service.chzzk;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.application.port.in.chzzk.HandleChzzkEventUseCase.SystemReceived;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort.SessionListResult;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort.SessionResult;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkConfigurationPort;

@ExtendWith(MockitoExtension.class)
class SystemServiceTest {

    @Mock
    private ChzzkConfigurationPort chzzkConfigurationPort;

    @Mock
    private ChzzkClientPort chzzkClientPort;

    @Test
    void handle_ShouldSubscribeChatAndDonationAndTrackConnectedSession() {
        SystemService service = new SystemService(chzzkConfigurationPort, chzzkClientPort);
        given(chzzkConfigurationPort.clientId()).willReturn("client");
        given(chzzkConfigurationPort.clientSecret()).willReturn("secret");
        given(chzzkClientPort.getSessionList("client", "secret")).willReturn(new SessionListResult(
                null,
                null,
                null,
                List.of(new SessionListResult.SessionData("session-1", null, null, List.of()))
        ));

        service.handle(new SystemReceived(
                "connected",
                new SystemReceived.SystemData("session-1", null, null)
        ));

        BDDMockito.then(chzzkClientPort).should().subscribeChatEvent("session-1");
        BDDMockito.then(chzzkClientPort).should().subscribeDonationEvent("session-1");
        then(service.isConnected()).isTrue();
    }

    @Test
    void handle_ShouldIgnoreConnectedEventWithoutSessionKey() {
        SystemService service = new SystemService(chzzkConfigurationPort, chzzkClientPort);

        service.handle(new SystemReceived(
                "connected",
                new SystemReceived.SystemData(" ", null, null)
        ));

        BDDMockito.then(chzzkClientPort).should(never()).subscribeChatEvent(org.mockito.ArgumentMatchers.any());
        BDDMockito.then(chzzkClientPort).should(never()).subscribeDonationEvent(org.mockito.ArgumentMatchers.any());
        then(service.isConnected()).isFalse();
    }

    @Test
    void getSession_ShouldRejectMissingUrl() {
        SystemService service = new SystemService(chzzkConfigurationPort, chzzkClientPort);
        given(chzzkConfigurationPort.clientId()).willReturn("client");
        given(chzzkConfigurationPort.clientSecret()).willReturn("secret");
        given(chzzkClientPort.getSession("client", "secret"))
                .willReturn(new SessionResult(" "));

        thenThrownBy(service::getSession)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("CHZZK session URL is missing");
    }
}
