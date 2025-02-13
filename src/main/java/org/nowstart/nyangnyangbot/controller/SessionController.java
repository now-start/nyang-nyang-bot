package org.nowstart.nyangnyangbot.controller;

import io.socket.client.IO;
import io.socket.client.Socket;
import java.net.URISyntaxException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.command.ChatListener;
import org.nowstart.nyangnyangbot.command.SystemListener;
import org.nowstart.nyangnyangbot.data.type.EventType;
import org.nowstart.nyangnyangbot.service.SessionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class SessionController {

    private final ChatListener chatListener;
    private final SystemListener systemListener;
    private final SessionService sessionService;

    //@Scheduled(fixedRate = 10000)
    @GetMapping("/connect")
    public void connect() throws URISyntaxException {
        Socket socket = IO.socket(sessionService.getSession(), IO.Options.builder()
                .setReconnection(false)
                .setTransports(new String[]{"websocket"})
                .build());

        socket.on(EventType.CHAT.name(), chatListener);
        socket.on(EventType.SYSTEM.name(), systemListener);

    }
}
