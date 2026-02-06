package org.nowstart.nyangnyangbot.service;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import io.socket.client.IO;
import io.socket.client.Socket;
import java.net.URISyntaxException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.data.type.EventType;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ChzzkConnectionServiceTest {

    @Mock
    private SystemService systemService;

    @Mock
    private ChatService chatService;

    @Mock
    private DefaultChzzkSocketFactory socketFactory;

    @Mock
    private Socket socket;

    @InjectMocks
    private ChzzkConnectionService chzzkConnectionService;

    @Test
    void connectIfNeeded_ShouldSkip_WhenAlreadyConnected() throws URISyntaxException {
        given(systemService.isConnected()).willReturn(true);

        chzzkConnectionService.connectIfNeeded();

        BDDMockito.then(socketFactory).shouldHaveNoInteractions();
        BDDMockito.then(systemService).should().isConnected();
    }

    @Test
    void connectIfNeeded_ShouldDisconnectExistingAndConnect_WhenNotConnected() throws URISyntaxException {
        Socket existingSocket = BDDMockito.mock(Socket.class);
        ReflectionTestUtils.setField(chzzkConnectionService, "socket", existingSocket);

        given(systemService.isConnected()).willReturn(false);
        given(systemService.getSession()).willReturn("wss://example");
        given(socketFactory.create(eq("wss://example"), any(IO.Options.class))).willReturn(socket);

        chzzkConnectionService.connectIfNeeded();

        ArgumentCaptor<IO.Options> optionsCaptor = ArgumentCaptor.forClass(IO.Options.class);
        BDDMockito.then(socketFactory).should().create(eq("wss://example"), optionsCaptor.capture());
        then(optionsCaptor.getValue().reconnection).isFalse();

        BDDMockito.then(existingSocket).should().disconnect();
        BDDMockito.then(socket).should().on(EventType.SYSTEM.name(), systemService);
        BDDMockito.then(socket).should().on(EventType.CHAT.name(), chatService);
        BDDMockito.then(socket).should().connect();
    }

    @Test
    void connectIfNeeded_ShouldConnect_WhenNoExistingSocket() throws URISyntaxException {
        given(systemService.isConnected()).willReturn(false);
        given(systemService.getSession()).willReturn("wss://example");
        given(socketFactory.create(eq("wss://example"), any(IO.Options.class))).willReturn(socket);

        chzzkConnectionService.connectIfNeeded();

        BDDMockito.then(socket).should().connect();
    }

    @Test
    void disconnect_ShouldHandleNullSocket() {
        chzzkConnectionService.disconnect();

        BDDMockito.then(socket).shouldHaveNoInteractions();
    }

    @Test
    void disconnect_ShouldDisconnectAndClearSocket() {
        ReflectionTestUtils.setField(chzzkConnectionService, "socket", socket);

        chzzkConnectionService.disconnect();

        BDDMockito.then(socket).should().disconnect();
        then(ReflectionTestUtils.getField(chzzkConnectionService, "socket")).isNull();
    }
}






