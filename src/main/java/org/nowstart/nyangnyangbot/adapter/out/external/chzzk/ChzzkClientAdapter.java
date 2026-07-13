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
import org.nowstart.nyangnyangbot.adapter.out.validation.OutboundContractValidator;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort;
import org.nowstart.nyangnyangbot.application.validation.outbound.ExternalResponseContractException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChzzkClientAdapter implements ChzzkClientPort {

    private final ChzzkOpenApiClient chzzkOpenApi;
    private final OutboundContractValidator contractValidator;

    @Override
    public AuthorizationToken getAccessToken(AuthorizationTokenCommand request) {
        contractValidator.request("chzzk.getAccessToken", request);
        return requireContent(
                "getAccessToken",
                chzzkOpenApi.getAccessToken(AuthorizationRequest.from(request)),
                AuthorizationResponse::toAuthorizationToken
        );
    }

    @Override
    public UserResult getUser(String authorization) {
        requireText("chzzk.getUser", "authorization", authorization);
        return requireContent("getUser", chzzkOpenApi.getUser(authorization), UserResponse::toUserResult);
    }

    @Override
    public void sendMessage(MessageCommand request) {
        contractValidator.request("chzzk.sendMessage", request);
        chzzkOpenApi.sendMessage(MessageRequest.from(request));
    }

    @Override
    public void subscribeChatEvent(String sessionKey) {
        requireText("chzzk.subscribeChatEvent", "sessionKey", sessionKey);
        chzzkOpenApi.subscribeChatEvent(sessionKey);
    }

    @Override
    public void subscribeDonationEvent(String sessionKey) {
        requireText("chzzk.subscribeDonationEvent", "sessionKey", sessionKey);
        chzzkOpenApi.subscribeDonationEvent(sessionKey);
    }

    @Override
    public SessionResult getSessionList(String clientId, String clientSecret) {
        requireClientCredentials("chzzk.getSessionList", clientId, clientSecret);
        return requireContent(
                "getSessionList",
                chzzkOpenApi.getSessionList(clientId, clientSecret),
                SessionResponse::toSessionResult
        );
    }

    @Override
    public SessionResult getSession(String clientId, String clientSecret) {
        requireClientCredentials("chzzk.getSession", clientId, clientSecret);
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
            throw new ExternalResponseContractException("CHZZK API request failed: operation=%s, code=%s"
                    .formatted(operation, code));
        }
        return contractValidator.externalResponse(operation, converter.apply(response.content()));
    }

    private void requireClientCredentials(String operation, String clientId, String clientSecret) {
        requireText(operation, "clientId", clientId);
        requireText(operation, "clientSecret", clientSecret);
    }

    private void requireText(String operation, String field, String value) {
        contractValidator.request(operation + "." + field, new RequiredText(value));
    }

    private record RequiredText(
            @jakarta.validation.constraints.NotBlank(message = "value is required") String value
    ) {
    }
}
