package org.nowstart.nyangnyangbot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URISyntaxException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.service.ChzzkConnectionService;
import org.nowstart.nyangnyangbot.service.LeaderElectionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/chzzk")
@Tag(name = "Chzzk Chat", description = "치지직 채팅 소켓 연결 관리 API")
public class ChzzkController {

    private final ChzzkConnectionService connectionService;
    private final LeaderElectionService leaderElectionService;

    @Operation(
            summary = "치지직 채팅 연결",
            description = "치지직 채팅 소켓에 연결합니다. 1분마다 연결 상태를 확인하여 자동으로 재연결됩니다."
    )
    @GetMapping("/connect")
    public String connect() throws URISyntaxException {
        if (leaderElectionService.isLeader()) {
            connectionService.connectIfNeeded();
        } else {
            log.info("[ChzzkChat][SKIP] not a leader");
        }

        return "SUCCESS";
    }
}
