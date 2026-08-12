package org.nowstart.nyangnyangbot.application.port.out.chzzk;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.List;

public interface ChzzkClientPort {

    /** 인가 요청으로 OAuth 액세스 토큰을 발급받는다. */
    @Valid
    AuthorizationToken getAccessToken(
            @Valid @NotNull(message = "authorization request is required") AuthorizationTokenCommand request
    );

    /** 인가 정보에 해당하는 CHZZK 사용자를 반환한다. */
    @Valid
    UserResult getUser(@NotBlank(message = "authorization is required") String authorization);

    /** 설정된 CHZZK 채널로 채팅 메시지를 전송한다. */
    void sendMessage(@Valid @NotNull(message = "message request is required") MessageCommand request);

    /** 세션이 채팅 이벤트를 수신하도록 구독한다. */
    void subscribeChatEvent(@NotBlank(message = "sessionKey is required") String sessionKey);

    /** 세션이 후원 이벤트를 수신하도록 구독한다. */
    void subscribeDonationEvent(@NotBlank(message = "sessionKey is required") String sessionKey);

    /** 전달된 클라이언트 인증 정보로 사용 가능한 세션 목록을 반환한다. */
    @Valid
    SessionListResult getSessionList(
            @NotBlank(message = "clientId is required") String clientId,
            @NotBlank(message = "clientSecret is required") String clientSecret
    );

    /** 전달된 클라이언트 인증 정보로 채팅 세션을 반환한다. */
    @Valid
    SessionResult getSession(
            @NotBlank(message = "clientId is required") String clientId,
            @NotBlank(message = "clientSecret is required") String clientSecret
    );

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
            @NotBlank(message = "url is required")
            String url
    ) {
    }

    record SessionListResult(
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
