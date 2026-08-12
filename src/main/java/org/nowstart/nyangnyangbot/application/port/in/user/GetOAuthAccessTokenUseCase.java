package org.nowstart.nyangnyangbot.application.port.in.user;

import org.nowstart.nyangnyangbot.application.port.out.user.OAuthCredentialPort.OAuthCredentialRecord;

public interface GetOAuthAccessTokenUseCase {

    /** 현재 사용자의 사용 가능한 OAuth 인증 정보를 반환하며, 필요한 경우 토큰을 갱신한다. */
    OAuthCredentialRecord getAccessToken();
}
