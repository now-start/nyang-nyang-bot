package org.nowstart.nyangnyangbot.controller;

import static io.socket.client.IO.Options;
import static io.socket.client.IO.socket;

import io.socket.client.Socket;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URISyntaxException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.data.type.EventType;
import org.nowstart.nyangnyangbot.service.ChatService;
import org.nowstart.nyangnyangbot.service.SystemService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/chzzk")
@Tag(name = "Chzzk Chat", description = "치지직 채팅 소켓 연결 관리 API")
public class ChzzkController {

    private final SystemService systemService;
    private final ChatService chatService;
    private Socket socket;

    @Operation(
            summary = "치지직 채팅 연결",
            description = "치지직 채팅 소켓에 연결합니다. 1분마다 연결 상태를 확인하여 자동으로 재연결됩니다."
    )
    @Scheduled(fixedDelay = 1000 * 60)
    public void scheduledConnect() throws URISyntaxException {
        connectInternal();
    }

    @GetMapping("/connect")
    @PreAuthorize("hasRole('ADMIN')")
    public String connect() throws URISyntaxException {
        connectInternal();
        return "SUCCESS";
    }

    private void connectInternal() throws URISyntaxException {
        if (systemService.isConnected()) {
            return;
        }

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
}

