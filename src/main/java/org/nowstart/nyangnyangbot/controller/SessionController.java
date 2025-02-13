package org.nowstart.nyangnyangbot.controller;

import io.socket.client.IO;
import io.socket.client.Socket;
import java.net.URISyntaxException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.data.type.EventType;
import org.nowstart.nyangnyangbot.service.ChatService;
import org.nowstart.nyangnyangbot.service.SessionService;
import org.nowstart.nyangnyangbot.service.SystemService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class SessionController {

    private final ChatService chatService;
    private final SystemService systemService;
    private final SessionService sessionService;

    //@Scheduled(fixedRate = 10000)
    @GetMapping("/connect")
    public String connect() throws URISyntaxException {
        Socket socket = IO.socket(sessionService.getSession());

        socket.on(EventType.SYSTEM.name(), systemService);
        socket.on(EventType.CHAT.name(), chatService);
        socket.connect();

        return "SUCCESS";
    }
}
