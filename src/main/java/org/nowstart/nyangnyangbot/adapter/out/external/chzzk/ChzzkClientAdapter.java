package org.nowstart.nyangnyangbot.adapter.out.external.chzzk;

import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.adapter.out.external.chzzk.client.ChzzkOpenApiClient;
import org.nowstart.nyangnyangbot.adapter.out.external.chzzk.request.AuthorizationRequest;
import org.nowstart.nyangnyangbot.adapter.out.external.chzzk.request.MessageRequest;
import org.nowstart.nyangnyangbot.adapter.out.external.chzzk.response.AuthorizationResponse;
import org.nowstart.nyangnyangbot.adapter.out.external.chzzk.response.SessionResponse;
import org.nowstart.nyangnyangbot.adapter.out.external.chzzk.response.UserResponse;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChzzkClientAdapter implements ChzzkClientPort {

    private final ChzzkOpenApiClient chzzkOpenApi;

    @Override
    public ApiResult<AuthorizationToken> getAccessToken(AuthorizationTokenCommand request) {
        return chzzkOpenApi.getAccessToken(AuthorizationRequest.from(request))
                .toApiResult(AuthorizationResponse::toAuthorizationToken);
    }

    @Override
    public ApiResult<UserResult> getUser(String authorization) {
        return chzzkOpenApi.getUser(authorization)
                .toApiResult(UserResponse::toUserResult);
    }

    @Override
    public void sendMessage(MessageCommand request) {
        chzzkOpenApi.sendMessage(MessageRequest.from(request));
    }

    @Override
    public void subscribeChatEvent(String sessionKey) {
        chzzkOpenApi.subscribeChatEvent(sessionKey);
    }

    @Override
    public void subscribeDonationEvent(String sessionKey) {
        chzzkOpenApi.subscribeDonationEvent(sessionKey);
    }

    @Override
    public void subscribeSubscriptionEvent(String sessionKey) {
        chzzkOpenApi.subscribeSubscriptionEvent(sessionKey);
    }

    @Override
    public ApiResult<SessionResult> getSessionList(String clientId, String clientSecret) {
        return chzzkOpenApi.getSessionList(clientId, clientSecret)
                .toApiResult(SessionResponse::toSessionResult);
    }

    @Override
    public ApiResult<SessionResult> getSession(String clientId, String clientSecret) {
        return chzzkOpenApi.getSession(clientId, clientSecret)
                .toApiResult(SessionResponse::toSessionResult);
    }
}
