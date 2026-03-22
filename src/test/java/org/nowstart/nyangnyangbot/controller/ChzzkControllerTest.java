package org.nowstart.nyangnyangbot.controller;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.times;
import static org.mockito.Mockito.doReturn;

import io.socket.client.Socket;
import java.net.URISyntaxException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.service.ChatService;
import org.nowstart.nyangnyangbot.service.DonationService;
import org.nowstart.nyangnyangbot.service.SubscriptionService;
import org.nowstart.nyangnyangbot.service.SystemService;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class ChzzkControllerTest {

    @Mock
    private SystemService systemService;

    @Mock
    private ChatService chatService;

    @Mock
    private DonationService donationService;

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private Socket socket;

    private ChzzkController chzzkController;

    private ChzzkController createController() throws URISyntaxException {
        ChzzkController controller = BDDMockito.spy(
                new ChzzkController(systemService, chatService, donationService, subscriptionService)
        );
        doReturn(socket).when(controller).createSocket(anyString(), any());
        return controller;
    }

    private ChzzkController createControllerWithoutSocketStub() {
        return new ChzzkController(systemService, chatService, donationService, subscriptionService);
    }

    @Test
    void connect_ShouldReturnSuccess_WhenNotConnected() throws URISyntaxException {
        // given
        chzzkController = createController();
        given(systemService.isConnected()).willReturn(false);
        given(systemService.getSession()).willReturn("https://example.com");

        // when
        ResponseEntity<String> result = chzzkController.connect();

        // then
        then(result.getStatusCode().is2xxSuccessful()).isTrue();
        then(result.getBody()).isEqualTo("SUCCESS");
        BDDMockito.then(systemService).should().isConnected();
        BDDMockito.then(systemService).should().getSession();
    }

    @Test
    void connect_ShouldReturnSuccess_WhenAlreadyConnected() throws URISyntaxException {
        // given
        chzzkController = createControllerWithoutSocketStub();
        given(systemService.isConnected()).willReturn(true);

        // when
        ResponseEntity<String> result = chzzkController.connect();

        // then
        then(result.getStatusCode().is2xxSuccessful()).isTrue();
        then(result.getBody()).isEqualTo("SUCCESS");
        BDDMockito.then(systemService).should().isConnected();
        BDDMockito.then(systemService).should(never()).getSession();
    }

    @Test
    void connect_ShouldCallSystemService_WhenNotConnected() throws URISyntaxException {
        // given
        chzzkController = createController();
        given(systemService.isConnected()).willReturn(false);
        given(systemService.getSession()).willReturn("https://example.com");

        // when
        chzzkController.connect();

        // then
        BDDMockito.then(systemService).should().isConnected();
        BDDMockito.then(systemService).should().getSession();
    }

    @Test
    void connect_ShouldNotReconnect_WhenAlreadyConnected() throws URISyntaxException {
        // given
        chzzkController = createControllerWithoutSocketStub();
        given(systemService.isConnected()).willReturn(true);

        // when
        ResponseEntity<String> result = chzzkController.connect();

        // then
        then(result.getStatusCode().is2xxSuccessful()).isTrue();
        then(result.getBody()).isEqualTo("SUCCESS");
        BDDMockito.then(systemService).should(times(1)).isConnected();
        BDDMockito.then(systemService).shouldHaveNoMoreInteractions();
    }

    @Test
    void connect_ShouldHandleMultipleCalls() throws URISyntaxException {
        // given
        chzzkController = createController();
        given(systemService.isConnected())
                .willReturn(false)
                .willReturn(true);
        given(systemService.getSession()).willReturn("https://example.com");

        // when
        ResponseEntity<String> result1 = chzzkController.connect();
        ResponseEntity<String> result2 = chzzkController.connect();

        // then
        then(result1.getStatusCode().is2xxSuccessful()).isTrue();
        then(result1.getBody()).isEqualTo("SUCCESS");
        then(result2.getStatusCode().is2xxSuccessful()).isTrue();
        then(result2.getBody()).isEqualTo("SUCCESS");
        BDDMockito.then(systemService).should(times(2)).isConnected();
        BDDMockito.then(systemService).should(times(1)).getSession();
    }

    @Test
    void connect_ShouldGetNewSession_EachTimeWhenDisconnected() throws URISyntaxException {
        // given
        chzzkController = createController();
        given(systemService.isConnected()).willReturn(false);
        given(systemService.getSession()).willReturn("https://example.com");

        // when
        chzzkController.connect();
        chzzkController.connect();

        // then
        BDDMockito.then(systemService).should(times(2)).isConnected();
        BDDMockito.then(systemService).should(times(2)).getSession();
    }
}
