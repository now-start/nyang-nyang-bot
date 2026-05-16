package org.nowstart.nyangnyangbot.application.port.out.chzzk;

import static org.assertj.core.api.BDDAssertions.then;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort.ApiResult;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort.AuthorizationToken;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort.AuthorizationTokenCommand;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort.ChatEventPayload;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort.SessionResult;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort.SubscriptionEventPayload;
import org.junit.jupiter.api.Test;

class ChzzkDtoDeserializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void sessionDto_ShouldAllowNullPaginationFields() throws Exception {
        // 준비
        String json = """
                {
                  "code": 200,
                  "message": "OK",
                  "content": {
                    "url": "wss://example",
                    "page": null,
                    "totalCount": null,
                    "totalPages": null,
                    "data": null
                  }
                }
                """;

        // 실행
        ApiResult<SessionResult> response = objectMapper.readValue(
                json,
                new TypeReference<ApiResult<SessionResult>>() {
                }
        );

        // 검증
        then(response.code()).isEqualTo(200);
        then(response.content().page()).isNull();
        then(response.content().totalCount()).isNull();
        then(response.content().totalPages()).isNull();
    }

    @Test
    void authorizationDto_ShouldAllowNullExpiresIn() throws Exception {
        // 준비
        String json = """
                {
                  "accessToken": "access",
                  "refreshToken": "refresh",
                  "tokenType": "Bearer",
                  "expiresIn": null,
                  "scope": "chat"
                }
                """;

        // 실행
        AuthorizationToken response = objectMapper.readValue(json, AuthorizationToken.class);

        // 검증
        then(response.expiresIn()).isNull();
    }

    @Test
    void authorizationDto_ToStringShouldMaskSensitiveTokens() {
        // 준비
        AuthorizationToken dto = new AuthorizationToken("access-secret", "refresh-secret", "Bearer", 3600, "chat");

        // 실행
        String result = dto.toString();

        // 검증
        then(result).doesNotContain("access-secret", "refresh-secret");
        then(result).contains("accessToken=<masked>", "refreshToken=<masked>");
    }

    @Test
    void authorizationRequestDto_ToStringShouldMaskSensitiveValues() {
        // 준비
        AuthorizationTokenCommand dto = new AuthorizationTokenCommand(
                "authorization_code",
                "client-id",
                "client-secret",
                "code-secret",
                "state-secret",
                "refresh-secret"
        );

        // 실행
        String result = dto.toString();

        // 검증
        then(result).doesNotContain("client-secret", "code-secret", "state-secret", "refresh-secret");
        then(result).contains(
                "grantType=authorization_code",
                "clientId=client-id",
                "clientSecret=<masked>",
                "code=<masked>",
                "state=<masked>",
                "refreshToken=<masked>"
        );
    }

    @Test
    void subscriptionDto_ShouldAllowNullTierFields() throws Exception {
        // 준비
        String json = """
                {
                  "channelId": "channel-1",
                  "subscriberChannelId": "user-1",
                  "subscriberNickname": "tester",
                  "tierNo": null,
                  "tierName": "unknown",
                  "month": null
                }
                """;

        // 실행
        SubscriptionEventPayload response = objectMapper.readValue(json, SubscriptionEventPayload.class);

        // 검증
        then(response.tierNo()).isNull();
        then(response.month()).isNull();
    }

    @Test
    void chatDto_ShouldAllowNullMessageTime() throws Exception {
        // 준비
        String json = """
                {
                  "channelId": "channel-1",
                  "senderChannelId": "user-1",
                  "profile": {
                    "nickname": "tester",
                    "badges": [],
                    "verifiedMark": true
                  },
                  "content": "hello",
                  "emojis": {},
                  "messageTime": null
                }
                """;

        // 실행
        ChatEventPayload response = objectMapper.readValue(json, ChatEventPayload.class);

        // 검증
        then(response.messageTime()).isNull();
    }
}
