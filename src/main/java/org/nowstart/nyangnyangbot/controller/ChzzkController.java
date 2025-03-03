package org.nowstart.nyangnyangbot.controller;

import io.socket.client.IO;
import io.socket.client.Socket;
import java.net.URISyntaxException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.data.property.ChzzkProperty;
import org.nowstart.nyangnyangbot.data.type.EventType;
import org.nowstart.nyangnyangbot.service.ChatService;
import org.nowstart.nyangnyangbot.service.SystemService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/chzzk")
public class ChzzkController {

    private final ChzzkProperty chzzkProperty;
    private final SystemService systemService;
    private final ChatService chatService;
    private Socket socket;

    @GetMapping("/connect")
    @Scheduled(fixedDelay = 1000 * 60)
    public String connect() throws URISyntaxException {
        if (!systemService.isConnected(chzzkProperty)) {
            log.info("[ChzzkChat][START]");
            IO.Options option = new IO.Options();
            option.reconnection = false;

            socket = IO.socket(systemService.getSession(chzzkProperty), option);

            socket.on(EventType.SYSTEM.name(), systemService);
            socket.on(EventType.CHAT.name(), chatService);
            socket.connect();
        }

        return "SUCCESS";
    }
}
