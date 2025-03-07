package org.nowstart.nyangnyangbot.controller;

import io.socket.client.Socket;
import java.net.URISyntaxException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.data.type.EventType;
import org.nowstart.nyangnyangbot.service.ChatService;
import org.nowstart.nyangnyangbot.service.SystemService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static io.socket.client.IO.Options;
import static io.socket.client.IO.socket;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/chzzk")
public class ChzzkController {

    private Socket socket;
    private final SystemService systemService;
    private final ChatService chatService;

    @GetMapping("/connect")
    @Scheduled(fixedDelay = 1000 * 60)
    public String connect() throws URISyntaxException {
        if (!systemService.isConnected()) {
            log.info("[ChzzkChat][START]");

            if (socket != null) {
                socket.disconnect();
            }

            Options option = new Options();
            option.reconnection = false;

            socket = socket(systemService.getSession(), option);

            socket.on(EventType.SYSTEM.name(), systemService);
            socket.on(EventType.CHAT.name(), chatService);
            socket.connect();
        }

        return "SUCCESS";
    }
}
