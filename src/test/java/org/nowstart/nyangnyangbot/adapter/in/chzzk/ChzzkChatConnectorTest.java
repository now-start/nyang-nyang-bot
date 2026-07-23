package org.nowstart.nyangnyangbot.adapter.in.chzzk;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.times;
import static org.mockito.Mockito.doReturn;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import java.net.URISyntaxException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.application.port.in.chzzk.ConnectChzzkChatUseCase;
import org.nowstart.nyangnyangbot.application.port.in.chzzk.HandleChzzkEventUseCase;

@ExtendWith(MockitoExtension.class)
class ChzzkChatConnectorTest {

    @Mock
    private ConnectChzzkChatUseCase connectChzzkChatUseCase;

    @Mock
    private HandleChzzkEventUseCase handleChzzkEventUseCase;

    @Mock
    private Socket socket;

    private ChzzkChatConnector createConnector() throws URISyntaxException {
        ChzzkChatConnector connector = BDDMockito.spy(new ChzzkChatConnector(
                connectChzzkChatUseCase,
                handleChzzkEventUseCase,
                new ObjectMapper()
        ));
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
        ChzzkChatConnector connector = new ChzzkChatConnector(
                connectChzzkChatUseCase,
                handleChzzkEventUseCase,
                new ObjectMapper()
        );
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

        // 실행
        connector.connect();
        connector.connect();

        // 검증
        BDDMockito.then(connectChzzkChatUseCase).should(times(2)).isConnected();
        BDDMockito.then(connectChzzkChatUseCase).should(times(2)).getSession();
    }

    @Test
    @DisplayName("소켓 연결 시 시스템과 채팅 및 후원 이벤트를 구독한다")
    void connect_ShouldSubscribeSocketEvents_WhenSocketConnects() throws URISyntaxException {
        // 준비
        ChzzkChatConnector connector = createConnector();
        given(connectChzzkChatUseCase.isConnected()).willReturn(false);
        given(connectChzzkChatUseCase.getSession()).willReturn("https://example.com");

        // 실행
        connector.connect();

        // 검증
        BDDMockito.then(socket).should().on(eq("SYSTEM"), any(Emitter.Listener.class));
        BDDMockito.then(socket).should().on(eq("CHAT"), any(Emitter.Listener.class));
        BDDMockito.then(socket).should().on(eq("DONATION"), any(Emitter.Listener.class));
    }

    @Test
    @DisplayName("채팅 소켓 JSON을 애플리케이션 이벤트로 변환한다")
    void connect_ShouldDispatchChatPayload() throws URISyntaxException {
        // 준비
        ChzzkChatConnector connector = createConnector();
        given(connectChzzkChatUseCase.isConnected()).willReturn(false);
        given(connectChzzkChatUseCase.getSession()).willReturn("https://example.com");
        connector.connect();
        ArgumentCaptor<Emitter.Listener> listenerCaptor = ArgumentCaptor.forClass(Emitter.Listener.class);
        BDDMockito.then(socket).should().on(eq("CHAT"), listenerCaptor.capture());

        // 실행
        listenerCaptor.getValue().call("""
                {
                  "channelId": "channel-1",
                  "senderChannelId": "user-1",
                  "profile": {"nickname": "치즈냥", "badges": [], "verifiedMark": true},
                  "content": "안녕",
                  "emojis": {},
                  "messageTime": 1711111111
                }
                """);

        // 검증
        BDDMockito.then(handleChzzkEventUseCase).should().handleChatEvent(BDDMockito.argThat(event ->
                event.senderChannelId().equals("user-1")
                        && event.profile().nickname().equals("치즈냥")
                        && event.content().equals("안녕")
        ));
    }

    @Test
    @DisplayName("공식 후원 소켓 JSON에 수신 키를 부여해 애플리케이션으로 전달한다")
    void connect_ShouldDispatchOfficialDonationPayloadWithGeneratedIngestionKey() throws URISyntaxException {
        ChzzkChatConnector connector = createConnector();
        given(connectChzzkChatUseCase.isConnected()).willReturn(false);
        given(connectChzzkChatUseCase.getSession()).willReturn("https://example.com");
        doReturn("chzzk-received:test").when(connector).nextDonationIngestionKey();
        connector.connect();
        ArgumentCaptor<Emitter.Listener> listenerCaptor = ArgumentCaptor.forClass(Emitter.Listener.class);
        BDDMockito.then(socket).should().on(eq("DONATION"), listenerCaptor.capture());

        listenerCaptor.getValue().call("""
                {
                  "donationType": "CHAT",
                  "channelId": "streamer-1",
                  "donatorChannelId": "viewer-1",
                  "donatorNickname": "치즈냥",
                  "payAmount": "10,000",
                  "donationText": "!룰렛",
                  "emojis": {}
                }
                """);

        BDDMockito.then(handleChzzkEventUseCase).should().handleDonationEvent(BDDMockito.argThat(event ->
                "chzzk-received:test".equals(event.ingestionKey())
                        && "streamer-1".equals(event.channelId())
                        && "viewer-1".equals(event.donatorChannelId())
                        && "10,000".equals(event.payAmount())
        ));
    }

    @Test
    @DisplayName("잘못된 소켓 payload는 애플리케이션으로 전달하지 않는다")
    void connect_ShouldIgnoreMalformedPayload() throws URISyntaxException {
        // 준비
        ChzzkChatConnector connector = createConnector();
        given(connectChzzkChatUseCase.isConnected()).willReturn(false);
        given(connectChzzkChatUseCase.getSession()).willReturn("https://example.com");
        connector.connect();
        ArgumentCaptor<Emitter.Listener> listenerCaptor = ArgumentCaptor.forClass(Emitter.Listener.class);
        BDDMockito.then(socket).should().on(eq("CHAT"), listenerCaptor.capture());

        // 실행
        listenerCaptor.getValue().call("not-json");

        // 검증
        BDDMockito.then(handleChzzkEventUseCase).shouldHaveNoInteractions();
    }
}
