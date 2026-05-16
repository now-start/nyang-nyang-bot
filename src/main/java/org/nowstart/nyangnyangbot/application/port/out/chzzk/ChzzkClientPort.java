package org.nowstart.nyangnyangbot.application.port.out.chzzk;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.util.List;
import java.util.Map;

public interface ChzzkClientPort {

    ApiResult<AuthorizationToken> getAccessToken(AuthorizationTokenCommand request);

    ApiResult<UserResult> getUser(String authorization);

    void sendMessage(MessageCommand request);

    void subscribeChatEvent(String sessionKey);

    void subscribeDonationEvent(String sessionKey);

    void subscribeSubscriptionEvent(String sessionKey);

    ApiResult<SessionResult> getSessionList(String clientId, String clientSecret);

    ApiResult<SessionResult> getSession(String clientId, String clientSecret);

    record ApiResult<T>(Integer code, String message, T content) {
    }

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

    record ChatEventPayload(
            String channelId,
            String senderChannelId,
            Profile profile,
            String content,
            Map<String, String> emojis,
            Long messageTime
    ) {
        public record Profile(
                String nickname,
                List<Map<String, String>> badges,
                Boolean verifiedMark
        ) {
        }
    }

    record DonationEventPayload(
            @JsonAlias({"donationId", "eventId", "id"})
            String donationEventId,
            String donationType,
            String channelId,
            String donatorChannelId,
            String donatorNickname,
            String payAmount,
            String donationText,
            Map<String, String> emojis
    ) {
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

    record SubscriptionEventPayload(
            String channelId,
            String subscriberChannelId,
            String subscriberNickname,
            Integer tierNo,
            String tierName,
            Integer month
    ) {
    }

    record SystemEventPayload(String type, SystemData data) {

        public record SystemData(String sessionKey, String eventType, String channelId) {
        }
    }

    record UserResult(String channelId, String channelName, String status) {
    }
}
