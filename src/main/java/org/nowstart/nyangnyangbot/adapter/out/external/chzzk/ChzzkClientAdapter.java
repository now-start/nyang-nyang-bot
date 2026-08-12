package org.nowstart.nyangnyangbot.adapter.out.external.chzzk;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.adapter.out.external.chzzk.client.ChzzkOpenApiClient;
import org.nowstart.nyangnyangbot.adapter.out.external.chzzk.request.AuthorizationRequest;
import org.nowstart.nyangnyangbot.adapter.out.external.chzzk.request.MessageRequest;
import org.nowstart.nyangnyangbot.adapter.out.external.chzzk.response.AuthorizationResponse;
import org.nowstart.nyangnyangbot.adapter.out.external.chzzk.response.ChzzkApiResponse;
import org.nowstart.nyangnyangbot.adapter.out.external.chzzk.response.SessionResponse;
import org.nowstart.nyangnyangbot.adapter.out.external.chzzk.response.UserResponse;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort;
import org.nowstart.nyangnyangbot.application.exception.ExternalSystemException;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Validated
@RequiredArgsConstructor
public class ChzzkClientAdapter implements ChzzkClientPort {

    private final ChzzkOpenApiClient chzzkOpenApi;

    @Override
    public AuthorizationToken getAccessToken(AuthorizationTokenCommand request) {
        return requireContent(
                "getAccessToken",
                execute("getAccessToken", () -> chzzkOpenApi.getAccessToken(AuthorizationRequest.from(request))),
                AuthorizationResponse::toAuthorizationToken
        );
    }

    @Override
    public UserResult getUser(String authorization) {
        return requireContent(
                "getUser",
                execute("getUser", () -> chzzkOpenApi.getUser(authorization)),
                UserResponse::toUserResult
        );
    }

    @Override
    public void sendMessage(MessageCommand request) {
        execute("sendMessage", () -> chzzkOpenApi.sendMessage(MessageRequest.from(request)));
    }

    @Override
    public void subscribeChatEvent(String sessionKey) {
        execute("subscribeChatEvent", () -> chzzkOpenApi.subscribeChatEvent(sessionKey));
    }

    @Override
    public void subscribeDonationEvent(String sessionKey) {
        execute("subscribeDonationEvent", () -> chzzkOpenApi.subscribeDonationEvent(sessionKey));
    }

    @Override
    public SessionListResult getSessionList(String clientId, String clientSecret) {
        return requireContent(
                "getSessionList",
                execute("getSessionList", () -> chzzkOpenApi.getSessionList(clientId, clientSecret)),
                SessionResponse::toSessionListResult
        );
    }

    @Override
    public SessionResult getSession(String clientId, String clientSecret) {
        return requireContent(
                "getSession",
                execute("getSession", () -> chzzkOpenApi.getSession(clientId, clientSecret)),
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
            throw new ExternalSystemException("CHZZK API request failed: operation=%s, code=%s"
                    .formatted(operation, code));
        }
        return converter.apply(response.content());
    }

    private <T> T execute(String operation, Supplier<T> request) {
        try {
            return request.get();
        } catch (ExternalSystemException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new ExternalSystemException("CHZZK API request failed: operation=" + operation, exception);
        }
    }

    private void execute(String operation, Runnable request) {
        execute(operation, () -> {
            request.run();
            return null;
        });
    }

}
