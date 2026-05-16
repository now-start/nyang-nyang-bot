package org.nowstart.nyangnyangbot.application.auth;

import org.nowstart.nyangnyangbot.application.service.authorization.OAuthStateService;
import static org.assertj.core.api.BDDAssertions.then;

import org.junit.jupiter.api.Test;

class OAuthStateServiceTest {

    private final OAuthStateService oAuthStateService = new OAuthStateService();

    @Test
    void generateState_ShouldReturnUrlSafeRandomValue() {
        // 실행
        String first = oAuthStateService.generateState();
        String second = oAuthStateService.generateState();

        // 검증
        then(first).matches("[A-Za-z0-9_-]{43}");
        then(second).matches("[A-Za-z0-9_-]{43}");
        then(first).isNotEqualTo(second);
    }

    @Test
    void matches_ShouldRequireExactNonNullState() {
        // 실행 및 검증
        then(oAuthStateService.matches("state", "state")).isTrue();
        then(oAuthStateService.matches("state", "wrong")).isFalse();
        then(oAuthStateService.matches(null, "state")).isFalse();
        then(oAuthStateService.matches("state", null)).isFalse();
    }
}
