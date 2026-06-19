package org.nowstart.nyangnyangbot.adapter.in.web.chzzk;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URISyntaxException;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.application.port.in.chzzk.ConnectChzzkChatSocketUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/chzzk")
@Tag(name = "Chzzk Chat", description = "치지직 채팅 소켓 연결 관리 API")
public class ChzzkController {

    private final ConnectChzzkChatSocketUseCase connectChzzkChatSocketUseCase;

    @Operation(summary = "치지직 채팅 수동 연결")
    @GetMapping("/connect")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> connect() throws URISyntaxException {
        connectChzzkChatSocketUseCase.connect();
        return ResponseEntity.ok("SUCCESS");
    }
}
