package org.nowstart.nyangnyangbot.application.service.chzzk;

import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkChatSocketPort;

@ExtendWith(MockitoExtension.class)
class ChzzkChatConnectionServiceTest {

    @Mock
    private SystemService systemService;

    @Mock
    private ChzzkChatSocketPort chzzkChatSocketPort;

    @Test
    void connect_ShouldOpenSocketWithNewSession_WhenDisconnected() {
        ChzzkChatConnectionService service = new ChzzkChatConnectionService(systemService, chzzkChatSocketPort);
        given(systemService.beginConnection()).willReturn(1L);
        given(systemService.getSession()).willReturn("https://example.com");

        service.connect();

        BDDMockito.then(chzzkChatSocketPort).should().connect("https://example.com", 1L);
    }

    @Test
    void connect_ShouldNotOpenSocket_WhenConnectionIsAlreadyActive() {
        ChzzkChatConnectionService service = new ChzzkChatConnectionService(systemService, chzzkChatSocketPort);
        given(systemService.beginConnection()).willReturn(0L);

        service.connect();

        BDDMockito.then(systemService).should(never()).getSession();
        BDDMockito.then(chzzkChatSocketPort).shouldHaveNoInteractions();
    }

    @Test
    void connect_ShouldReleaseConnectionState_WhenOpeningSocketFails() {
        ChzzkChatConnectionService service = new ChzzkChatConnectionService(systemService, chzzkChatSocketPort);
        given(systemService.beginConnection()).willReturn(1L);
        given(systemService.getSession()).willReturn("https://example.com");
        IllegalStateException failure = new IllegalStateException("socket failure");
        BDDMockito.willThrow(failure).given(chzzkChatSocketPort).connect("https://example.com", 1L);

        thenThrownBy(service::connect).isSameAs(failure);

        BDDMockito.then(systemService).should().handleConnectionClosed(1L);
    }
}
