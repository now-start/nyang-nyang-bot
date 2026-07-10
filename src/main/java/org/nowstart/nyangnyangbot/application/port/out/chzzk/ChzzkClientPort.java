package org.nowstart.nyangnyangbot.application.port.out.chzzk;

import java.util.List;

public interface ChzzkClientPort {

    AuthorizationToken getAccessToken(AuthorizationTokenCommand request);

    UserResult getUser(String authorization);

    void sendMessage(MessageCommand request);

    void subscribeChatEvent(String sessionKey);

    void subscribeDonationEvent(String sessionKey);

    void subscribeSubscriptionEvent(String sessionKey);

    SessionResult getSessionList(String clientId, String clientSecret);

    SessionResult getSession(String clientId, String clientSecret);

    record AuthorizationToken(
            String accessToken,
            String refreshToken,
            String tokenType,
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
            String grantType,
            String clientId,
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

    record MessageCommand(String message) {
    }

    record SessionResult(
            String url,
            Integer page,
            Integer totalCount,
            Integer totalPages,
            List<SessionData> data
    ) {
        public record SessionData(
                String sessionKey,
                String connectedDate,
                String disconnectedDate,
                List<SubscribedEvents> subscribedEvents
        ) {
            public record SubscribedEvents(String eventType, String channelId) {
            }
        }
    }

    record UserResult(String channelId, String channelName, String status) {
    }
}
