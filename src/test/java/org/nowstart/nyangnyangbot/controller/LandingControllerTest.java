package org.nowstart.nyangnyangbot.controller;

import static org.assertj.core.api.BDDAssertions.then;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.ModelAndView;

class LandingControllerTest {

    private final LandingController landingController = new LandingController();

    @Test
    void landing_ShouldReturnIndexViewInLandingMode() {
        ModelAndView result = landingController.landing();

        then(result.getViewName()).isEqualTo("index");
        then(result.getModel().get("landingMode")).isEqualTo(true);
    }
}
