package org.nowstart.nyangnyangbot.application.port.out.chzzk;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.List;

public interface ChzzkClientPort {

    AuthorizationToken getAccessToken(AuthorizationTokenCommand request);

    UserResult getUser(String authorization);

    void sendMessage(MessageCommand request);

    void subscribeChatEvent(String sessionKey);

    void subscribeDonationEvent(String sessionKey);

    SessionResult getSessionList(String clientId, String clientSecret);

    SessionResult getSession(String clientId, String clientSecret);

    record AuthorizationToken(
            @NotBlank(message = "accessToken is required")
            String accessToken,
            @NotBlank(message = "refreshToken is required")
            String refreshToken,
            @NotBlank(message = "tokenType is required")
            String tokenType,
            @NotNull(message = "expiresIn is required")
            @Positive(message = "expiresIn must be positive")
            Integer expiresIn,
            String scope
    ) {

        @Override
        public String toString() {
            return "AuthorizationToken[accessToken=<masked>, refreshToken=<masked>, tokenType=%s, expiresIn=%s, scope=%s]"
                    .formatted(tokenType, expiresIn, scope);
        }
    }

    record AuthorizationTokenCommand(
            @NotBlank(message = "grantType is required")
            String grantType,
            @NotBlank(message = "clientId is required")
            String clientId,
            @NotBlank(message = "clientSecret is required")
            String clientSecret,
            String code,
            String state,
            String refreshToken
    ) {

        @Override
        public String toString() {
            return "AuthorizationTokenCommand[grantType=%s, clientId=%s, clientSecret=<masked>, code=<masked>, state=<masked>, refreshToken=<masked>]"
                    .formatted(grantType, clientId);
        }
    }

    record MessageCommand(
            @NotBlank(message = "message is required")
            String message
    ) {
    }

    record SessionResult(
            String url,
            @PositiveOrZero(message = "page must not be negative")
            Integer page,
            @PositiveOrZero(message = "totalCount must not be negative")
            Integer totalCount,
            @PositiveOrZero(message = "totalPages must not be negative")
            Integer totalPages,
            @NotNull(message = "data is required")
            List<@Valid @NotNull(message = "session data is required") SessionData> data
    ) {
        public record SessionData(
                @NotBlank(message = "sessionKey is required")
                String sessionKey,
                String connectedDate,
                String disconnectedDate,
                @NotNull(message = "subscribedEvents is required")
                List<@Valid @NotNull(message = "subscribed event is required") SubscribedEvents> subscribedEvents
        ) {
            public record SubscribedEvents(
                    @NotBlank(message = "eventType is required") String eventType,
                    @NotBlank(message = "channelId is required") String channelId
            ) {
            }
        }
    }

    record UserResult(
            @NotBlank(message = "channelId is required") String channelId,
            @NotBlank(message = "channelName is required") String channelName,
            String status
    ) {
    }
}
