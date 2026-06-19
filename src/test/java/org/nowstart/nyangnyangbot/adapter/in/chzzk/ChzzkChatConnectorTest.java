package org.nowstart.nyangnyangbot.adapter.in.chzzk;

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

@ExtendWith(MockitoExtension.class)
class ChzzkChatConnectorTest {

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

    private ChzzkChatConnector createConnector() throws URISyntaxException {
        ChzzkChatConnector connector = BDDMockito.spy(new ChzzkChatConnector(connectChzzkChatUseCase));
        doReturn(socket).when(connector).createSocket(anyString(), any());
        return connector;
    }

    @Test
    @DisplayName("미연결 상태에서 connect 시 세션을 획득하고 소켓을 연결한다")
    void connect_ShouldCreateSocket_WhenNotConnected() throws URISyntaxException {
        // 준비
        ChzzkChatConnector connector = createConnector();
        given(connectChzzkChatUseCase.isConnected()).willReturn(false);
        given(connectChzzkChatUseCase.getSession()).willReturn("https://example.com");
        given(connectChzzkChatUseCase.systemListener()).willReturn(systemListener);
        given(connectChzzkChatUseCase.chatListener()).willReturn(chatListener);
        given(connectChzzkChatUseCase.donationListener()).willReturn(donationListener);

        // 실행
        connector.connect();

        // 검증
        BDDMockito.then(connectChzzkChatUseCase).should().isConnected();
        BDDMockito.then(connectChzzkChatUseCase).should().getSession();
        BDDMockito.then(socket).should().connect();
    }

    @Test
    @DisplayName("이미 연결된 상태에서는 재연결하지 않는다")
    void connect_ShouldNotReconnect_WhenAlreadyConnected() throws URISyntaxException {
        // 준비
        ChzzkChatConnector connector = new ChzzkChatConnector(connectChzzkChatUseCase);
        given(connectChzzkChatUseCase.isConnected()).willReturn(true);

        // 실행
        connector.connect();

        // 검증
        BDDMockito.then(connectChzzkChatUseCase).should(times(1)).isConnected();
        BDDMockito.then(connectChzzkChatUseCase).should(never()).getSession();
        BDDMockito.then(connectChzzkChatUseCase).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("connect를 여러 번 호출할 때 연결 상태에 따라 세션 획득을 조절한다")
    void connect_ShouldHandleMultipleCalls() throws URISyntaxException {
        // 준비
        ChzzkChatConnector connector = createConnector();
        given(connectChzzkChatUseCase.isConnected())
                .willReturn(false)
                .willReturn(true);
        given(connectChzzkChatUseCase.getSession()).willReturn("https://example.com");
        given(connectChzzkChatUseCase.systemListener()).willReturn(systemListener);
        given(connectChzzkChatUseCase.chatListener()).willReturn(chatListener);
        given(connectChzzkChatUseCase.donationListener()).willReturn(donationListener);

        // 실행
        connector.connect();
        connector.connect();

        // 검증
        BDDMockito.then(connectChzzkChatUseCase).should(times(2)).isConnected();
        BDDMockito.then(connectChzzkChatUseCase).should(times(1)).getSession();
    }

    @Test
    @DisplayName("미연결 상태에서 connect 할 때마다 새로운 세션을 획득한다")
    void connect_ShouldGetNewSession_EachTimeWhenDisconnected() throws URISyntaxException {
        // 준비
        ChzzkChatConnector connector = createConnector();
        given(connectChzzkChatUseCase.isConnected()).willReturn(false);
        given(connectChzzkChatUseCase.getSession()).willReturn("https://example.com");
        given(connectChzzkChatUseCase.systemListener()).willReturn(systemListener);
        given(connectChzzkChatUseCase.chatListener()).willReturn(chatListener);
        given(connectChzzkChatUseCase.donationListener()).willReturn(donationListener);

        // 실행
        connector.connect();
        connector.connect();

        // 검증
        BDDMockito.then(connectChzzkChatUseCase).should(times(2)).isConnected();
        BDDMockito.then(connectChzzkChatUseCase).should(times(2)).getSession();
    }

    @Test
    @DisplayName("소켓 연결 시 시스템, 채팅, 후원 이벤트를 구독한다")
    void connect_ShouldSubscribeSocketEvents_WhenSocketConnects() throws URISyntaxException {
        // 준비
        ChzzkChatConnector connector = createConnector();
        given(connectChzzkChatUseCase.isConnected()).willReturn(false);
        given(connectChzzkChatUseCase.getSession()).willReturn("https://example.com");
        given(connectChzzkChatUseCase.systemListener()).willReturn(systemListener);
        given(connectChzzkChatUseCase.chatListener()).willReturn(chatListener);
        given(connectChzzkChatUseCase.donationListener()).willReturn(donationListener);

        // 실행
        connector.connect();

        // 검증
        BDDMockito.then(socket).should().on(ConnectChzzkChatUseCase.SYSTEM_EVENT_NAME, systemListener);
        BDDMockito.then(socket).should().on(ConnectChzzkChatUseCase.CHAT_EVENT_NAME, chatListener);
        BDDMockito.then(socket).should().on(ConnectChzzkChatUseCase.DONATION_EVENT_NAME, donationListener);
    }
}
