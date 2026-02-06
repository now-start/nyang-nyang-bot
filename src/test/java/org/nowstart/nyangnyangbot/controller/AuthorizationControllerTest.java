package org.nowstart.nyangnyangbot.controller;

import static org.assertj.core.api.BDDAssertions.then;

import org.junit.jupiter.api.Test;
class AuthorizationControllerTest {

    private final AuthorizationController authorizationController = new AuthorizationController();

    @Test
    void login_ShouldRedirectToOAuth2Authorization() {
        String result = authorizationController.login();

        then(result).isEqualTo("redirect:/oauth2/authorization/chzzk");
    }
}






