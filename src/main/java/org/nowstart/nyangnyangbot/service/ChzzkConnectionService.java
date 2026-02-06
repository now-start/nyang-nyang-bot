package org.nowstart.nyangnyangbot.service;

import io.socket.client.IO.Options;
import io.socket.client.Socket;
import java.net.URISyntaxException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.data.type.EventType;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChzzkConnectionService {

    private final SystemService systemService;
    private final ChatService chatService;
    private final DefaultChzzkSocketFactory socketFactory;
    private Socket socket;

    public synchronized void connectIfNeeded() throws URISyntaxException {
        if (systemService.isConnected()) {
            return;
        }

        log.info("[ChzzkChat][START]");

        if (socket != null) {
            socket.disconnect();
        }

        Options option = new Options();
        option.reconnection = false;

        socket = socketFactory.create(systemService.getSession(), option);

        socket.on(EventType.SYSTEM.name(), systemService);
        socket.on(EventType.CHAT.name(), chatService);
        socket.connect();
    }

    public synchronized void disconnect() {
        if (socket != null) {
            socket.disconnect();
            socket = null;
        }
    }
}






