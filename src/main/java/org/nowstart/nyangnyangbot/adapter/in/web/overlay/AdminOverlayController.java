package org.nowstart.nyangnyangbot.adapter.in.web.overlay;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.adapter.in.web.overlay.response.OverlayEventResponse;
import org.nowstart.nyangnyangbot.adapter.in.web.overlay.response.OverlayTokenIssueResponse;
import org.nowstart.nyangnyangbot.application.port.in.overlay.IssueOverlayTokenUseCase;
import org.nowstart.nyangnyangbot.application.port.in.overlay.ManageOverlayDisplayUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/overlay/roulette")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Overlay API", description = "관리자 OBS 오버레이 API")
public class AdminOverlayController {

    private final IssueOverlayTokenUseCase issueOverlayTokenUseCase;
    private final ManageOverlayDisplayUseCase manageOverlayDisplayUseCase;

    @Operation(summary = "오버레이 토큰 발급")
    @PostMapping("/token")
    public ResponseEntity<OverlayTokenIssueResponse> issueToken(Authentication authentication) {
        return ResponseEntity.ok(OverlayTokenIssueResponse.from(
                issueOverlayTokenUseCase.issueToken(authentication.getName())
        ));
    }

    @Operation(summary = "룰렛 오버레이 재송출")
    @PostMapping("/events/{rouletteEventId}/replay")
    public ResponseEntity<OverlayEventResponse> replay(@PathVariable Long rouletteEventId) {
        return ResponseEntity.ok(OverlayEventResponse.from(
                manageOverlayDisplayUseCase.replayRouletteEvent(rouletteEventId)
        ));
    }
}
