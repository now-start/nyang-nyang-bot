package org.nowstart.nyangnyangbot.adapter.out.external.chzzk;

import java.util.Objects;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.adapter.out.external.chzzk.client.ChzzkOpenApiClient;
import org.nowstart.nyangnyangbot.adapter.out.external.chzzk.request.AuthorizationRequest;
import org.nowstart.nyangnyangbot.adapter.out.external.chzzk.request.MessageRequest;
import org.nowstart.nyangnyangbot.adapter.out.external.chzzk.response.AuthorizationResponse;
import org.nowstart.nyangnyangbot.adapter.out.external.chzzk.response.ChzzkApiResponse;
import org.nowstart.nyangnyangbot.adapter.out.external.chzzk.response.SessionResponse;
import org.nowstart.nyangnyangbot.adapter.out.external.chzzk.response.UserResponse;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChzzkClientAdapter implements ChzzkClientPort {

    private final ChzzkOpenApiClient chzzkOpenApi;

    @Override
    public AuthorizationToken getAccessToken(AuthorizationTokenCommand request) {
        return requireContent(
                "getAccessToken",
                chzzkOpenApi.getAccessToken(AuthorizationRequest.from(request)),
                AuthorizationResponse::toAuthorizationToken
        );
    }

    @Override
    public UserResult getUser(String authorization) {
        return requireContent("getUser", chzzkOpenApi.getUser(authorization), UserResponse::toUserResult);
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
    public SessionResult getSessionList(String clientId, String clientSecret) {
        return requireContent(
                "getSessionList",
                chzzkOpenApi.getSessionList(clientId, clientSecret),
                SessionResponse::toSessionResult
        );
    }

    @Override
    public SessionResult getSession(String clientId, String clientSecret) {
        return requireContent(
                "getSession",
                chzzkOpenApi.getSession(clientId, clientSecret),
                SessionResponse::toSessionResult
        );
    }

    private <T, R> R requireContent(
            String operation,
            ChzzkApiResponse<T> response,
            Function<T, R> converter
    ) {
        if (response == null || !Objects.equals(response.code(), 200) || response.content() == null) {
            Integer code = response == null ? null : response.code();
            throw new IllegalStateException("CHZZK API request failed: operation=%s, code=%s"
                    .formatted(operation, code));
        }
        return converter.apply(response.content());
    }
}
