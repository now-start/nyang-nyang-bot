package org.nowstart.nyangnyangbot.controller;

import io.socket.client.IO;
import io.socket.client.Socket;
import jakarta.annotation.PostConstruct;
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

    @PostConstruct
    public void init() throws URISyntaxException {
        log.info("[ChzzkChat][START]");
        IO.Options option = new IO.Options();
        option.reconnection = false;

        Socket socket = IO.socket(systemService.getSession(chzzkProperty), option);
        socket.on(EventType.SYSTEM.name(), systemService);
        socket.on(EventType.CHAT.name(), chatService);
        socket.connect();
    }

    @GetMapping("/connect")
    @Scheduled(fixedDelay = 1000 * 60)
    public String connect() {
        try {
            systemService.subscribeChatEvent();
        } catch (Exception e) {
            log.error("[ChzzkChat][ERROR] : ", e);
        }

        return "SUCCESS";
    }

    @GetMapping("/reconnect")
    public String reconnect() throws URISyntaxException {
        init();

        return "SUCCESS";
    }
}
