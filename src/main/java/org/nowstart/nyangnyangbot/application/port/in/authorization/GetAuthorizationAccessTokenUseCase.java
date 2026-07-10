package org.nowstart.nyangnyangbot.application.port.in.authorization;

import org.nowstart.nyangnyangbot.application.port.out.authorization.AuthorizationPort.AuthorizationAccountResult;

public interface GetAuthorizationAccessTokenUseCase {

    AuthorizationAccountResult getAccessToken();
}
