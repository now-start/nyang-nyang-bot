package org.nowstart.nyangnyangbot.application.port.in.user;

import org.nowstart.nyangnyangbot.application.port.out.user.OAuthCredentialPort.OAuthCredentialRecord;

public interface GetOAuthAccessTokenUseCase {

    OAuthCredentialRecord getAccessToken();
}
