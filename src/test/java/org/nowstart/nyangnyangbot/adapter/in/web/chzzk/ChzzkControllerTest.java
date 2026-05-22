package org.nowstart.nyangnyangbot.adapter.in.web.chzzk;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.times;
import static org.mockito.Mockito.doReturn;

import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import java.net.URISyntaxException;
import org.junit.jupiter.api.DisplayName;
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
    @DisplayName("미연결 상태에서 connect를 호출하면 SUCCESS를 반환한다")
    void connect_ShouldReturnSuccess_WhenNotConnected() throws URISyntaxException {
        // 준비
        chzzkController = createController();
        given(connectChzzkChatUseCase.isConnected()).willReturn(false);
        given(connectChzzkChatUseCase.getSession()).willReturn("https://example.com");
        given(connectChzzkChatUseCase.systemListener()).willReturn(systemListener);
        given(connectChzzkChatUseCase.chatListener()).willReturn(chatListener);
        given(connectChzzkChatUseCase.donationListener()).willReturn(donationListener);

        // 실행
        ResponseEntity<String> result = chzzkController.connect();

        // 검증
        then(result.getStatusCode().is2xxSuccessful()).isTrue();
        then(result.getBody()).isEqualTo("SUCCESS");
        BDDMockito.then(connectChzzkChatUseCase).should().isConnected();
        BDDMockito.then(connectChzzkChatUseCase).should().getSession();
    }

    @Test
    @DisplayName("이미 연결된 상태에서 connect를 호출하면 SUCCESS를 반환한다")
    void connect_ShouldReturnSuccess_WhenAlreadyConnected() throws URISyntaxException {
        // 준비
        chzzkController = createControllerWithoutSocketStub();
        given(connectChzzkChatUseCase.isConnected()).willReturn(true);

        // 실행
        ResponseEntity<String> result = chzzkController.connect();

        // 검증
        then(result.getStatusCode().is2xxSuccessful()).isTrue();
        then(result.getBody()).isEqualTo("SUCCESS");
        BDDMockito.then(connectChzzkChatUseCase).should().isConnected();
        BDDMockito.then(connectChzzkChatUseCase).should(never()).getSession();
    }

    @Test
    @DisplayName("미연결 상태에서 connect 시 시스템 서비스를 호출한다")
    void connect_ShouldCallSystemService_WhenNotConnected() throws URISyntaxException {
        // 준비
        chzzkController = createController();
        given(connectChzzkChatUseCase.isConnected()).willReturn(false);
        given(connectChzzkChatUseCase.getSession()).willReturn("https://example.com");
        given(connectChzzkChatUseCase.systemListener()).willReturn(systemListener);
        given(connectChzzkChatUseCase.chatListener()).willReturn(chatListener);
        given(connectChzzkChatUseCase.donationListener()).willReturn(donationListener);

        // 실행
        chzzkController.connect();

        // 검증
        BDDMockito.then(connectChzzkChatUseCase).should().isConnected();
        BDDMockito.then(connectChzzkChatUseCase).should().getSession();
    }

    @Test
    @DisplayName("이미 연결된 상태에서는 재연결하지 않는다")
    void connect_ShouldNotReconnect_WhenAlreadyConnected() throws URISyntaxException {
        // 준비
        chzzkController = createControllerWithoutSocketStub();
        given(connectChzzkChatUseCase.isConnected()).willReturn(true);

        // 실행
        ResponseEntity<String> result = chzzkController.connect();

        // 검증
        then(result.getStatusCode().is2xxSuccessful()).isTrue();
        then(result.getBody()).isEqualTo("SUCCESS");
        BDDMockito.then(connectChzzkChatUseCase).should(times(1)).isConnected();
        BDDMockito.then(connectChzzkChatUseCase).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("connect를 여러 번 호출할 때 연결 상태에 따라 세션 획득을 조절한다")
    void connect_ShouldHandleMultipleCalls() throws URISyntaxException {
        // 준비
        chzzkController = createController();
        given(connectChzzkChatUseCase.isConnected())
                .willReturn(false)
                .willReturn(true);
        given(connectChzzkChatUseCase.getSession()).willReturn("https://example.com");
        given(connectChzzkChatUseCase.systemListener()).willReturn(systemListener);
        given(connectChzzkChatUseCase.chatListener()).willReturn(chatListener);
        given(connectChzzkChatUseCase.donationListener()).willReturn(donationListener);

        // 실행
        ResponseEntity<String> result1 = chzzkController.connect();
        ResponseEntity<String> result2 = chzzkController.connect();

        // 검증
        then(result1.getStatusCode().is2xxSuccessful()).isTrue();
        then(result1.getBody()).isEqualTo("SUCCESS");
        then(result2.getStatusCode().is2xxSuccessful()).isTrue();
        then(result2.getBody()).isEqualTo("SUCCESS");
        BDDMockito.then(connectChzzkChatUseCase).should(times(2)).isConnected();
        BDDMockito.then(connectChzzkChatUseCase).should(times(1)).getSession();
    }

    @Test
    @DisplayName("미연결 상태에서 connect 할 때마다 새로운 세션을 획득한다")
    void connect_ShouldGetNewSession_EachTimeWhenDisconnected() throws URISyntaxException {
        // 준비
        chzzkController = createController();
        given(connectChzzkChatUseCase.isConnected()).willReturn(false);
        given(connectChzzkChatUseCase.getSession()).willReturn("https://example.com");
        given(connectChzzkChatUseCase.systemListener()).willReturn(systemListener);
        given(connectChzzkChatUseCase.chatListener()).willReturn(chatListener);
        given(connectChzzkChatUseCase.donationListener()).willReturn(donationListener);

        // 실행
        chzzkController.connect();
        chzzkController.connect();

        // 검증
        BDDMockito.then(connectChzzkChatUseCase).should(times(2)).isConnected();
        BDDMockito.then(connectChzzkChatUseCase).should(times(2)).getSession();
    }

    @Test
    @DisplayName("소켓 연결 시 후원 이벤트를 구독한다")
    void connect_ShouldSubscribeDonationEvent_WhenSocketConnects() throws URISyntaxException {
        // 준비
        chzzkController = createController();
        given(connectChzzkChatUseCase.isConnected()).willReturn(false);
        given(connectChzzkChatUseCase.getSession()).willReturn("https://example.com");
        given(connectChzzkChatUseCase.systemListener()).willReturn(systemListener);
        given(connectChzzkChatUseCase.chatListener()).willReturn(chatListener);
        given(connectChzzkChatUseCase.donationListener()).willReturn(donationListener);

        // 실행
        chzzkController.connect();

        // 검증
        BDDMockito.then(socket).should().on(ConnectChzzkChatUseCase.DONATION_EVENT_NAME, donationListener);
    }
}
