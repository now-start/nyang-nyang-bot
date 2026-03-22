package org.nowstart.nyangnyangbot.controller;

import static org.assertj.core.api.BDDAssertions.then;

import org.junit.jupiter.api.Test;

class LandingControllerTest {

    private final LandingController landingController = new LandingController();

    @Test
    void landing_ShouldReturnLandingView() {
        String viewName = landingController.landing();

        then(viewName).isEqualTo("landing");
    }
}
