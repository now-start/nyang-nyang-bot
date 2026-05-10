package org.nowstart.nyangnyangbot.adapter.in.web;

import static org.assertj.core.api.BDDAssertions.then;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.ModelAndView;

class OverlayControllerTest {

    @Test
    void rouletteOverlay_ShouldReturnOverlayTemplate() {
        OverlayController controller = new OverlayController();

        ModelAndView result = controller.rouletteOverlay();

        then(result.getViewName()).isEqualTo("overlay-roulette");
    }
}
