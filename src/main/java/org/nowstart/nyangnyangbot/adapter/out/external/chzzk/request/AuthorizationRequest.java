package org.nowstart.nyangnyangbot.adapter.out.external.chzzk.request;

import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort.AuthorizationTokenCommand;

public record AuthorizationRequest(
        String grantType,
        String clientId,
        String clientSecret,
        String code,
        String state,
        String refreshToken
) {

    public static AuthorizationRequest from(AuthorizationTokenCommand command) {
        return new AuthorizationRequest(
                command.grantType(),
                command.clientId(),
                command.clientSecret(),
                command.code(),
                command.state(),
                command.refreshToken()
        );
    }
}
