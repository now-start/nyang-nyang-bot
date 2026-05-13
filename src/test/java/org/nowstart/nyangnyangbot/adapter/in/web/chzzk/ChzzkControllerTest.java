package org.nowstart.nyangnyangbot.adapter.in.web.chzzk;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.times;
import static org.mockito.Mockito.doReturn;

import io.socket.emitter.Emitter;
import io.socket.client.Socket;
import java.net.URISyntaxException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.application.port.in.chzzk.ConnectChzzkChatUseCase;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class ChzzkControllerTest {

    @Mock
    private ConnectChzzkChatUseCase connectChzzkChatUseCase;

    @Mock
    private Emitter.Listener systemListener;

    @Mock
    private Emitter.Listener chatListener;

    @Mock
    private Emitter.Listener donationListener;

    @Mock
    private Socket socket;

    private ChzzkController chzzkController;

    private ChzzkController createController() throws URISyntaxException {
        ChzzkController controller = BDDMockito.spy(
                new ChzzkController(connectChzzkChatUseCase)
        );
        doReturn(socket).when(controller).createSocket(anyString(), any());
        return controller;
    }

    private ChzzkController createControllerWithoutSocketStub() {
        return new ChzzkController(connectChzzkChatUseCase);
    }

    @Test
    void connect_ShouldReturnSuccess_WhenNotConnected() throws URISyntaxException {
        // given
        chzzkController = createController();
        given(connectChzzkChatUseCase.isConnected()).willReturn(false);
        given(connectChzzkChatUseCase.getSession()).willReturn("https://example.com");
        given(connectChzzkChatUseCase.systemListener()).willReturn(systemListener);
        given(connectChzzkChatUseCase.chatListener()).willReturn(chatListener);
        given(connectChzzkChatUseCase.donationListener()).willReturn(donationListener);

        // when
        ResponseEntity<String> result = chzzkController.connect();

        // then
        then(result.getStatusCode().is2xxSuccessful()).isTrue();
        then(result.getBody()).isEqualTo("SUCCESS");
        BDDMockito.then(connectChzzkChatUseCase).should().isConnected();
        BDDMockito.then(connectChzzkChatUseCase).should().getSession();
    }

    @Test
    void connect_ShouldReturnSuccess_WhenAlreadyConnected() throws URISyntaxException {
        // given
        chzzkController = createControllerWithoutSocketStub();
        given(connectChzzkChatUseCase.isConnected()).willReturn(true);

        // when
        ResponseEntity<String> result = chzzkController.connect();

        // then
        then(result.getStatusCode().is2xxSuccessful()).isTrue();
        then(result.getBody()).isEqualTo("SUCCESS");
        BDDMockito.then(connectChzzkChatUseCase).should().isConnected();
        BDDMockito.then(connectChzzkChatUseCase).should(never()).getSession();
    }

    @Test
    void connect_ShouldCallSystemService_WhenNotConnected() throws URISyntaxException {
        // given
        chzzkController = createController();
        given(connectChzzkChatUseCase.isConnected()).willReturn(false);
        given(connectChzzkChatUseCase.getSession()).willReturn("https://example.com");
        given(connectChzzkChatUseCase.systemListener()).willReturn(systemListener);
        given(connectChzzkChatUseCase.chatListener()).willReturn(chatListener);
        given(connectChzzkChatUseCase.donationListener()).willReturn(donationListener);

        // when
        chzzkController.connect();

        // then
        BDDMockito.then(connectChzzkChatUseCase).should().isConnected();
        BDDMockito.then(connectChzzkChatUseCase).should().getSession();
    }

    @Test
    void connect_ShouldNotReconnect_WhenAlreadyConnected() throws URISyntaxException {
        // given
        chzzkController = createControllerWithoutSocketStub();
        given(connectChzzkChatUseCase.isConnected()).willReturn(true);

        // when
        ResponseEntity<String> result = chzzkController.connect();

        // then
        then(result.getStatusCode().is2xxSuccessful()).isTrue();
        then(result.getBody()).isEqualTo("SUCCESS");
        BDDMockito.then(connectChzzkChatUseCase).should(times(1)).isConnected();
        BDDMockito.then(connectChzzkChatUseCase).shouldHaveNoMoreInteractions();
    }

    @Test
    void connect_ShouldHandleMultipleCalls() throws URISyntaxException {
        // given
        chzzkController = createController();
        given(connectChzzkChatUseCase.isConnected())
                .willReturn(false)
                .willReturn(true);
        given(connectChzzkChatUseCase.getSession()).willReturn("https://example.com");
        given(connectChzzkChatUseCase.systemListener()).willReturn(systemListener);
        given(connectChzzkChatUseCase.chatListener()).willReturn(chatListener);
        given(connectChzzkChatUseCase.donationListener()).willReturn(donationListener);

        // when
        ResponseEntity<String> result1 = chzzkController.connect();
        ResponseEntity<String> result2 = chzzkController.connect();

        // then
        then(result1.getStatusCode().is2xxSuccessful()).isTrue();
        then(result1.getBody()).isEqualTo("SUCCESS");
        then(result2.getStatusCode().is2xxSuccessful()).isTrue();
        then(result2.getBody()).isEqualTo("SUCCESS");
        BDDMockito.then(connectChzzkChatUseCase).should(times(2)).isConnected();
        BDDMockito.then(connectChzzkChatUseCase).should(times(1)).getSession();
    }

    @Test
    void connect_ShouldGetNewSession_EachTimeWhenDisconnected() throws URISyntaxException {
        // given
        chzzkController = createController();
        given(connectChzzkChatUseCase.isConnected()).willReturn(false);
        given(connectChzzkChatUseCase.getSession()).willReturn("https://example.com");
        given(connectChzzkChatUseCase.systemListener()).willReturn(systemListener);
        given(connectChzzkChatUseCase.chatListener()).willReturn(chatListener);
        given(connectChzzkChatUseCase.donationListener()).willReturn(donationListener);

        // when
        chzzkController.connect();
        chzzkController.connect();

        // then
        BDDMockito.then(connectChzzkChatUseCase).should(times(2)).isConnected();
        BDDMockito.then(connectChzzkChatUseCase).should(times(2)).getSession();
    }

    @Test
    void connect_ShouldSubscribeDonationEvent_WhenSocketConnects() throws URISyntaxException {
        // given
        chzzkController = createController();
        given(connectChzzkChatUseCase.isConnected()).willReturn(false);
        given(connectChzzkChatUseCase.getSession()).willReturn("https://example.com");
        given(connectChzzkChatUseCase.systemListener()).willReturn(systemListener);
        given(connectChzzkChatUseCase.chatListener()).willReturn(chatListener);
        given(connectChzzkChatUseCase.donationListener()).willReturn(donationListener);

        // when
        chzzkController.connect();

        // then
        BDDMockito.then(socket).should().on(ConnectChzzkChatUseCase.DONATION_EVENT_NAME, donationListener);
    }
}
