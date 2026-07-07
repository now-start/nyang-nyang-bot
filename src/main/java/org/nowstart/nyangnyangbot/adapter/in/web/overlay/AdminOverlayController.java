package org.nowstart.nyangnyangbot.adapter.in.web.overlay;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.application.port.in.overlay.IssueOverlayTokenUseCase;
import org.nowstart.nyangnyangbot.application.port.in.overlay.ManageOverlayDisplayUseCase;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Controller
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/admin/overlay/roulette")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Overlay API", description = "관리자 OBS 오버레이 API")
public class AdminOverlayController {

    private static final String TOKEN_URL_FIELD_FRAGMENT = "features/roulette/components :: overlay-token-url-field";
    private static final String FEEDBACK_FRAGMENT = "components/feedback :: alert";

    private final IssueOverlayTokenUseCase issueOverlayTokenUseCase;
    private final ManageOverlayDisplayUseCase manageOverlayDisplayUseCase;

    @Operation(summary = "오버레이 토큰 발급")
    @PostMapping("/token")
    public String issueToken(
            Authentication authentication,
            HttpServletRequest request,
            Model model
    ) {
        var result = issueOverlayTokenUseCase.issueToken(authentication.getName());
        model.addAttribute("tokenUrl", overlayUrl(request, result.token()));
        return TOKEN_URL_FIELD_FRAGMENT;
    }

    @Operation(summary = "룰렛 오버레이 재송출")
    @PostMapping("/events/replay")
    public String replayFromForm(
            @RequestParam(required = false) Long rouletteEventId,
            Model model
    ) {
        return replayInternal(rouletteEventId, model);
    }

    @Operation(summary = "룰렛 오버레이 재송출")
    @PostMapping("/events/{rouletteEventId}/replay")
    public String replay(
            @PathVariable Long rouletteEventId,
            Model model
    ) {
        return replayInternal(rouletteEventId, model);
    }

    private String replayInternal(Long rouletteEventId, Model model) {
        try {
            manageOverlayDisplayUseCase.replayRouletteEvent(rouletteEventId);
            model.addAttribute("message", "오버레이 재송출 대기열에 추가했습니다.");
            model.addAttribute("tone", "success");
        } catch (RuntimeException e) {
            log.warn("Failed to replay roulette overlay event. rouletteEventId={}", rouletteEventId, e);
            model.addAttribute("message", "오버레이 재송출에 실패했습니다.");
            model.addAttribute("tone", "danger");
        }
        return FEEDBACK_FRAGMENT;
    }

    private String overlayUrl(HttpServletRequest request, String token) {
        String contextPath = request.getContextPath() == null ? "" : request.getContextPath();
        return ServletUriComponentsBuilder.fromRequestUri(request)
                .replacePath(contextPath + "/overlay/roulette")
                .replaceQuery(null)
                .fragment("token=" + token)
                .build()
                .encode()
                .toUriString();
    }
}
