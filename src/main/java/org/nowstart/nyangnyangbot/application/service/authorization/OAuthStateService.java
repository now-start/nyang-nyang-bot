package org.nowstart.nyangnyangbot.application.service.authorization;

import org.nowstart.nyangnyangbot.application.port.in.authorization.OAuthStateUseCase;
import org.nowstart.nyangnyangbot.domain.authorization.OAuthStatePolicy;
import org.springframework.stereotype.Service;

@Service
public class OAuthStateService implements OAuthStateUseCase {

    private final OAuthStatePolicy oAuthStatePolicy = new OAuthStatePolicy();

    @Override
    public String generateState() {
        return oAuthStatePolicy.generateState();
    }

    @Override
    public boolean matches(String expected, String actual) {
        return oAuthStatePolicy.matches(expected, actual);
    }
}
