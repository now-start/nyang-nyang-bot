package org.nowstart.nyangnyangbot.adapter.out.external.chzzk.response;

import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort.AuthorizationToken;

public record AuthorizationResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        Integer expiresIn,
        String scope
) {

    public AuthorizationToken toAuthorizationToken() {
        return new AuthorizationToken(
                accessToken,
                refreshToken,
                tokenType,
                expiresIn,
                scope
        );
    }
}
