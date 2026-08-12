package org.nowstart.nyangnyangbot.adapter.in.web.chzzk;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.application.port.in.chzzk.ConnectChzzkChatUseCase;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("/chzzk")
@Tag(name = "Chzzk Chat", description = "치지직 채팅 소켓 연결 관리 API")
public class ChzzkController {

    private static final String FEEDBACK_FRAGMENT = "components/feedback :: alert";

    private final ConnectChzzkChatUseCase connectChzzkChatUseCase;

    @Operation(summary = "치지직 채팅 수동 연결")
    @PostMapping("/connect")
    @PreAuthorize("hasRole('ADMIN')")
    public String connect(Model model) {
        connectChzzkChatUseCase.connect();
        model.addAttribute("message", "치지직 채팅 연결 완료");
        model.addAttribute("tone", "success");
        return FEEDBACK_FRAGMENT;
    }
}
