package org.nowstart.nyangnyangbot.adapter.in.web.overlay;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.adapter.in.web.overlay.response.OverlayEventResponse;
import org.nowstart.nyangnyangbot.application.port.in.overlay.ManageOverlayDisplayUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/overlay/roulette")
@Tag(name = "Overlay Roulette API", description = "OBS 룰렛 오버레이 이벤트 API")
public class OverlayRouletteApiController {

    private final ManageOverlayDisplayUseCase manageOverlayDisplayUseCase;

    @Operation(summary = "다음 오버레이 표시 이벤트 조회")
    @GetMapping("/events/next")
    public ResponseEntity<OverlayEventResponse> getNextEvent(
            @RequestHeader(name = "Authorization", required = false) String authorization
    ) {
        try {
            return manageOverlayDisplayUseCase.claimNextEvent(authorization)
                    .map(detail -> ResponseEntity.ok(OverlayEventResponse.from(detail)))
                    .orElseGet(() -> ResponseEntity.noContent().build());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @Operation(summary = "오버레이 표시 완료 처리")
    @PostMapping("/events/{displayEventId}/displayed")
    public ResponseEntity<Void> markDisplayed(
            @PathVariable Long displayEventId,
            @RequestHeader(name = "Authorization", required = false) String authorization
    ) {
        try {
            manageOverlayDisplayUseCase.markDisplayed(displayEventId, authorization);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }
}
