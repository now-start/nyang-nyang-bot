package org.nowstart.nyangnyangbot.data.dto.chzzk;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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

        ApiResponseDto<SessionDto> response = objectMapper.readValue(
                json,
                new TypeReference<ApiResponseDto<SessionDto>>() {
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

        AuthorizationDto response = objectMapper.readValue(json, AuthorizationDto.class);

        assertThat(response.expiresIn()).isNull();
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

        SubscriptionDto response = objectMapper.readValue(json, SubscriptionDto.class);

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

        ChatDto response = objectMapper.readValue(json, ChatDto.class);

        assertThat(response.messageTime()).isNull();
    }
}
