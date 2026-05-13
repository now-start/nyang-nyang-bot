package org.nowstart.nyangnyangbot.application.port.out.chzzk;

import static org.assertj.core.api.Assertions.assertThat;

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

        ApiResult<SessionResult> response = objectMapper.readValue(
                json,
                new TypeReference<ApiResult<SessionResult>>() {
                }
        );

        assertThat(response.code()).isEqualTo(200);
        assertThat(response.content().page()).isNull();
        assertThat(response.content().totalCount()).isNull();
        assertThat(response.content().totalPages()).isNull();
    }

    @Test
    void authorizationDto_ShouldAllowNullExpiresIn() throws Exception {
        String json = """
                {
                  "accessToken": "access",
                  "refreshToken": "refresh",
                  "tokenType": "Bearer",
                  "expiresIn": null,
                  "scope": "chat"
                }
                """;

        AuthorizationToken response = objectMapper.readValue(json, AuthorizationToken.class);

        assertThat(response.expiresIn()).isNull();
    }

    @Test
    void authorizationDto_ToStringShouldMaskSensitiveTokens() {
        AuthorizationToken dto = new AuthorizationToken("access-secret", "refresh-secret", "Bearer", 3600, "chat");

        String result = dto.toString();

        assertThat(result).doesNotContain("access-secret", "refresh-secret");
        assertThat(result).contains("accessToken=<masked>", "refreshToken=<masked>");
    }

    @Test
    void authorizationRequestDto_ToStringShouldMaskSensitiveValues() {
        AuthorizationTokenCommand dto = new AuthorizationTokenCommand(
                "authorization_code",
                "client-id",
                "client-secret",
                "code-secret",
                "state-secret",
                "refresh-secret"
        );

        String result = dto.toString();

        assertThat(result).doesNotContain("client-secret", "code-secret", "state-secret", "refresh-secret");
        assertThat(result).contains(
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

        SubscriptionEventPayload response = objectMapper.readValue(json, SubscriptionEventPayload.class);

        assertThat(response.tierNo()).isNull();
        assertThat(response.month()).isNull();
    }

    @Test
    void chatDto_ShouldAllowNullMessageTime() throws Exception {
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

        ChatEventPayload response = objectMapper.readValue(json, ChatEventPayload.class);

        assertThat(response.messageTime()).isNull();
    }
}
