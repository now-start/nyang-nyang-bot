package org.nowstart.nyangnyangbot.adapter.in.chzzk;

import static io.socket.client.IO.Options;
import static io.socket.client.IO.socket;

import io.socket.client.Socket;
import java.net.URISyntaxException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.application.port.in.chzzk.ConnectChzzkChatSocketUseCase;
import org.nowstart.nyangnyangbot.application.port.in.chzzk.ConnectChzzkChatUseCase;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChzzkChatConnector implements ConnectChzzkChatSocketUseCase {

    private final ConnectChzzkChatUseCase connectChzzkChatUseCase;
    private Socket socket;

    @Override
    public synchronized void connect() throws URISyntaxException {
        if (connectChzzkChatUseCase.isConnected()) {
            return;
        }

        log.info("[ChzzkChat][START]");

        if (socket != null) {
            socket.disconnect();
        }

        Options option = new Options();
        option.reconnection = false;

        socket = createSocket(connectChzzkChatUseCase.getSession(), option);

        socket.on(ConnectChzzkChatUseCase.SYSTEM_EVENT_NAME, connectChzzkChatUseCase.systemListener());
        socket.on(ConnectChzzkChatUseCase.CHAT_EVENT_NAME, connectChzzkChatUseCase.chatListener());
        socket.on(ConnectChzzkChatUseCase.DONATION_EVENT_NAME, connectChzzkChatUseCase.donationListener());
        // TODO: connect subscription event when ready.
        // socket.on(ConnectChzzkChatUseCase.SUBSCRIPTION_EVENT_NAME, connectChzzkChatUseCase.subscriptionListener());
        socket.connect();
    }

    Socket createSocket(String sessionUrl, Options options) throws URISyntaxException {
        return socket(sessionUrl, options);
    }
}
