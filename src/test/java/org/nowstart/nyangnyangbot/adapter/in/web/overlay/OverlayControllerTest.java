package org.nowstart.nyangnyangbot.adapter.in.web.overlay;

import static org.assertj.core.api.BDDAssertions.then;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.ModelAndView;

class OverlayControllerTest {

    @Test
    void rouletteOverlay_ShouldReturnOverlayTemplate() {
        // 준비
        OverlayController controller = new OverlayController();

        // 실행
        ModelAndView result = controller.rouletteOverlay();

        // 검증
        then(result.getViewName()).isEqualTo("overlay-roulette");
    }
}
