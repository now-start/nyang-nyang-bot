package org.nowstart.nyangnyangbot.adapter.in.web.overlay;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.BDDMockito.given;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.adapter.in.web.overlay.response.OverlayEventResponse;
import org.nowstart.nyangnyangbot.application.port.in.overlay.ManageOverlayDisplayUseCase.OverlayDisplayResult;
import org.nowstart.nyangnyangbot.application.service.overlay.OverlayDisplayService;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class OverlayRouletteApiControllerTest {

    @Mock
    private OverlayDisplayService overlayDisplayService;

    @Test
    void getNextEvent_ShouldReturnNoContentWhenQueueIsEmpty() {
        OverlayRouletteApiController controller = new OverlayRouletteApiController(overlayDisplayService);
        given(overlayDisplayService.claimNextEvent("Bearer token")).willReturn(Optional.empty());

        ResponseEntity<OverlayEventResponse> result = controller.getNextEvent("Bearer token");

        then(result.getStatusCode().value()).isEqualTo(204);
    }

    @Test
    void getNextEvent_ShouldReturnDisplayEvent() {
        OverlayRouletteApiController controller = new OverlayRouletteApiController(overlayDisplayService);
        OverlayDisplayResult detail = new OverlayDisplayResult(
                1L,
                20L,
                "치즈냥",
                1,
                5,
                LocalDateTime.of(2026, 5, 9, 12, 2),
                List.of()
        );
        given(overlayDisplayService.claimNextEvent("Bearer token")).willReturn(Optional.of(detail));

        ResponseEntity<OverlayEventResponse> result = controller.getNextEvent("Bearer token");

        then(result.getBody()).isEqualTo(OverlayEventResponse.from(detail));
    }

    @Test
    void markDisplayed_ShouldDelegateToService() {
        OverlayRouletteApiController controller = new OverlayRouletteApiController(overlayDisplayService);

        controller.markDisplayed(1L, "Bearer token");

        BDDMockito.then(overlayDisplayService).should().markDisplayed(1L, "Bearer token");
    }

    @Test
    void getNextEvent_ShouldReturnUnauthorizedForInvalidToken() {
        OverlayRouletteApiController controller = new OverlayRouletteApiController(overlayDisplayService);
        given(overlayDisplayService.claimNextEvent("Bearer bad"))
                .willThrow(new IllegalArgumentException("invalid overlay token"));

        ResponseEntity<OverlayEventResponse> result = controller.getNextEvent("Bearer bad");

        then(result.getStatusCode().value()).isEqualTo(401);
    }
}
