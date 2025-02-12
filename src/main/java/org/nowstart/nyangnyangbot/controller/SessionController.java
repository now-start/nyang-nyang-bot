package org.nowstart.nyangnyangbot.controller;

import io.socket.client.IO;
import io.socket.client.Socket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.command.ChatListener;
import org.nowstart.nyangnyangbot.command.SystemListener;
import org.nowstart.nyangnyangbot.data.type.EventType;
import org.nowstart.nyangnyangbot.service.SessionService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;

import java.net.URISyntaxException;

@Slf4j
@Controller
@RequiredArgsConstructor
public class SessionController {

    private final ChatListener chatListener;
    private final SystemListener systemListener;
    private final SessionService sessionService;

    //@Scheduled(fixedRate = 10000)
    public void connect() throws URISyntaxException {
        Socket socket = IO.socket(sessionService.getSession(), IO.Options.builder()
                .setReconnection(false)
                .setTransports(new String[]{"websocket"})
                .build());

        socket.on(EventType.SYSTEM.name(), chatListener);
        socket.on(EventType.SYSTEM.name(), systemListener);

    }
}
