package org.nowstart.nyangnyangbot.application.auth;

import org.nowstart.nyangnyangbot.application.service.authorization.OAuthStateService;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class OAuthStateServiceTest {

    private final OAuthStateService oAuthStateService = new OAuthStateService();

    @Test
    void generateState_ShouldReturnUrlSafeRandomValue() {
        String first = oAuthStateService.generateState();
        String second = oAuthStateService.generateState();

        assertThat(first).matches("[A-Za-z0-9_-]{43}");
        assertThat(second).matches("[A-Za-z0-9_-]{43}");
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void matches_ShouldRequireExactNonNullState() {
        assertThat(oAuthStateService.matches("state", "state")).isTrue();
        assertThat(oAuthStateService.matches("state", "wrong")).isFalse();
        assertThat(oAuthStateService.matches(null, "state")).isFalse();
        assertThat(oAuthStateService.matches("state", null)).isFalse();
    }
}
