package org.nowstart.nyangnyangbot.adapter.out.socket.chzzk;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.InOrder;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.application.port.in.chat.HandleChatEventUseCase;
import org.nowstart.nyangnyangbot.application.port.in.chzzk.HandleChzzkSystemEventUseCase;
import org.nowstart.nyangnyangbot.application.port.in.donation.HandleDonationEventUseCase;

@ExtendWith(MockitoExtension.class)
class ChzzkChatConnectorTest {

    @Mock
    private HandleChzzkSystemEventUseCase handleChzzkSystemEventUseCase;

    @Mock
    private HandleChatEventUseCase handleChatEventUseCase;

    @Mock
    private HandleDonationEventUseCase handleDonationEventUseCase;

    @Mock
    private Socket socket;

    @Mock
    private Socket replacementSocket;

    private ChzzkChatConnector createConnector() {
        ChzzkChatConnector connector = BDDMockito.spy(new ChzzkChatConnector(
                handleChzzkSystemEventUseCase,
                handleChatEventUseCase,
                handleDonationEventUseCase,
                new ObjectMapper()
        ));
        doReturn(socket).when(connector).createSocket(anyString(), any());
        return connector;
    }

    @Test
    @DisplayName("세션 URL로 소켓을 생성하고 연결한다")
    void connect_ShouldCreateSocket() {
        // 준비
        ChzzkChatConnector connector = createConnector();

        // 실행
        connector.connect("https://example.com", 1L);

        // 검증
        BDDMockito.then(connector).should().createSocket(eq("https://example.com"), any());
        BDDMockito.then(socket).should().connect();
    }

    @Test
    @DisplayName("소켓 연결 시 시스템과 채팅 및 후원 이벤트를 구독한다")
    void connect_ShouldSubscribeSocketEvents_WhenSocketConnects() {
        // 준비
        ChzzkChatConnector connector = createConnector();

        // 실행
        connector.connect("https://example.com", 1L);

        // 검증
        BDDMockito.then(socket).should().on(eq("SYSTEM"), any(Emitter.Listener.class));
        BDDMockito.then(socket).should().on(eq("CHAT"), any(Emitter.Listener.class));
        BDDMockito.then(socket).should().on(eq("DONATION"), any(Emitter.Listener.class));
        BDDMockito.then(socket).should().on(eq(Socket.EVENT_CONNECT_ERROR), any(Emitter.Listener.class));
        BDDMockito.then(socket).should().on(eq(Socket.EVENT_CONNECT_TIMEOUT), any(Emitter.Listener.class));
        BDDMockito.then(socket).should().on(eq(Socket.EVENT_DISCONNECT), any(Emitter.Listener.class));
    }

    @Test
    @DisplayName("소켓 연결 종료 시 연결 상태를 해제한다")
    void connect_ShouldReleaseConnectionState_WhenSocketDisconnects() {
        ChzzkChatConnector connector = createConnector();
        connector.connect("https://example.com", 1L);
        ArgumentCaptor<Emitter.Listener> disconnectListener = ArgumentCaptor.forClass(Emitter.Listener.class);
        ArgumentCaptor<Emitter.Listener> chatListener = ArgumentCaptor.forClass(Emitter.Listener.class);
        BDDMockito.then(socket).should().on(eq(Socket.EVENT_DISCONNECT), disconnectListener.capture());
        BDDMockito.then(socket).should().on(eq("CHAT"), chatListener.capture());

        disconnectListener.getValue().call("transport close");
        chatListener.getValue().call("{}");

        BDDMockito.then(handleChzzkSystemEventUseCase).should().handleConnectionClosed(1L);
        BDDMockito.then(handleChatEventUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("기존 소켓 교체 시 오래된 종료 콜백을 먼저 제거한다")
    void connect_ShouldRemoveOldListenersBeforeDisconnectingOldSocket() {
        ChzzkChatConnector connector = createConnector();
        connector.connect("https://example.com/first", 1L);
        BDDMockito.clearInvocations(socket);

        connector.connect("https://example.com/second", 2L);

        InOrder order = inOrder(socket);
        order.verify(socket).off();
        order.verify(socket).disconnect();
    }

    @Test
    @DisplayName("교체된 소켓의 오래된 종료 콜백에는 이전 연결 시도 ID를 유지한다")
    void connect_ShouldKeepOldAttemptIdInStaleDisconnectCallback() {
        ChzzkChatConnector connector = BDDMockito.spy(new ChzzkChatConnector(
                handleChzzkSystemEventUseCase,
                handleChatEventUseCase,
                handleDonationEventUseCase,
                new ObjectMapper()
        ));
        doReturn(socket, replacementSocket).when(connector).createSocket(anyString(), any());
        connector.connect("https://example.com/first", 1L);
        ArgumentCaptor<Emitter.Listener> oldListener = ArgumentCaptor.forClass(Emitter.Listener.class);
        BDDMockito.then(socket).should().on(eq(Socket.EVENT_DISCONNECT), oldListener.capture());
        connector.connect("https://example.com/second", 2L);

        oldListener.getValue().call("late disconnect");

        BDDMockito.then(handleChzzkSystemEventUseCase).should().handleConnectionClosed(1L);
        BDDMockito.then(handleChzzkSystemEventUseCase).should(never()).handleConnectionClosed(2L);
    }

    @Test
    @DisplayName("채팅 소켓 JSON을 애플리케이션 이벤트로 변환한다")
    void connect_ShouldDispatchChatPayload() {
        // 준비
        ChzzkChatConnector connector = createConnector();
        connector.connect("https://example.com", 1L);
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
        BDDMockito.then(handleChatEventUseCase).should().handle(BDDMockito.argThat(event ->
                event.senderChannelId().equals("user-1")
                        && event.profile().nickname().equals("치즈냥")
                        && event.content().equals("안녕")
        ));
    }

    @Test
    @DisplayName("공식 후원 소켓 JSON에 수신 키를 부여해 애플리케이션으로 전달한다")
    void connect_ShouldDispatchOfficialDonationPayloadWithGeneratedIngestionKey() {
        ChzzkChatConnector connector = createConnector();
        doReturn("chzzk-received:test").when(connector).nextDonationIngestionKey();
        connector.connect("https://example.com", 1L);
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

        BDDMockito.then(handleDonationEventUseCase).should().handle(BDDMockito.argThat(event ->
                "chzzk-received:test".equals(event.ingestionKey())
                        && "streamer-1".equals(event.channelId())
                        && "viewer-1".equals(event.donatorChannelId())
                        && "10,000".equals(event.payAmount())
        ));
    }

    @Test
    @DisplayName("잘못된 소켓 payload는 애플리케이션으로 전달하지 않는다")
    void connect_ShouldIgnoreMalformedPayload() {
        // 준비
        ChzzkChatConnector connector = createConnector();
        connector.connect("https://example.com", 1L);
        ArgumentCaptor<Emitter.Listener> listenerCaptor = ArgumentCaptor.forClass(Emitter.Listener.class);
        BDDMockito.then(socket).should().on(eq("CHAT"), listenerCaptor.capture());

        // 실행
        listenerCaptor.getValue().call("not-json");

        // 검증
        BDDMockito.then(handleChatEventUseCase).shouldHaveNoInteractions();
    }
}
