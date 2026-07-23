package org.nowstart.nyangnyangbot.adapter.in.web.overlay;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.application.port.in.overlay.ManageOverlayDisplayUseCase;
import org.nowstart.nyangnyangbot.application.port.in.overlay.ManageOverlayDisplayUseCase.OverlayDisplayResult;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequiredArgsConstructor
@Tag(name = "Overlay Page", description = "OBS 오버레이 페이지")
public class OverlayController {

    private static final String WAIT_FRAGMENT = "features/overlay/roulette :: overlay-wait";
    private static final String EVENT_FRAGMENT = "features/overlay/roulette :: overlay-event";
    private static final String ERROR_FRAGMENT = "features/overlay/roulette :: overlay-error";

    private final ManageOverlayDisplayUseCase manageOverlayDisplayUseCase;

    @Operation(summary = "룰렛 OBS 오버레이")
    @GetMapping("/overlay/roulette")
    public ModelAndView rouletteOverlay() {
        return new ModelAndView("overlay-roulette");
    }

    @Operation(summary = "다음 오버레이 표시 fragment 조회")
    @GetMapping("/overlay/roulette/jobs/next")
    public String nextJob(
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            Model model
    ) {
        try {
            return manageOverlayDisplayUseCase.claimNextJob(authorization(authorization))
                    .map(job -> overlayJob(job, model))
                    .orElse(WAIT_FRAGMENT);
        } catch (IllegalArgumentException exception) {
            model.addAttribute("message", "오버레이 토큰이 유효하지 않습니다.");
            return ERROR_FRAGMENT;
        }
    }

    @Operation(summary = "오버레이 표시 완료")
    @PostMapping("/overlay/roulette/jobs/{displayJobId}/displayed")
    public String markDisplayed(
            @PathVariable Long displayJobId,
            @RequestParam String claimToken,
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            Model model
    ) {
        try {
            manageOverlayDisplayUseCase.markDisplayed(
                    displayJobId,
                    claimToken,
                    authorization(authorization)
            );
            return WAIT_FRAGMENT;
        } catch (IllegalArgumentException | IllegalStateException exception) {
            model.addAttribute("message", "오버레이 표시 작업을 완료하지 못했습니다.");
            return ERROR_FRAGMENT;
        }
    }

    private String overlayJob(OverlayDisplayResult event, Model model) {
        model.addAttribute("event", event);
        return EVENT_FRAGMENT;
    }

    private String authorization(String authorization) {
        if (authorization == null || authorization.isBlank()) {
            throw new IllegalArgumentException("overlay authorization is required");
        }
        return authorization;
    }
}
