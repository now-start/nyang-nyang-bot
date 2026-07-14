package org.nowstart.nyangnyangbot.adapter.out.external.chzzk.response;

import static org.assertj.core.api.BDDAssertions.then;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort.AuthorizationToken;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort.AuthorizationTokenCommand;

class ChzzkDtoDeserializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void sessionCreationDto_ShouldMapUrlOnlyResponse() throws Exception {
        String json = """
                {
                  "code": 200,
                  "message": "OK",
                  "content": {
                    "url": "https://example"
                  }
                }
                """;

        ChzzkApiResponse<SessionResponse> response = objectMapper.readValue(
                json,
                new TypeReference<ChzzkApiResponse<SessionResponse>>() {
                }
        );

        then(response.code()).isEqualTo(200);
        then(response.content().toSessionResult().url()).isEqualTo("https://example");
    }

    @Test
    void sessionListDto_ShouldPreserveNullNestedFieldsForContractValidation() throws Exception {
        String json = """
                {
                  "code": 200,
                  "message": "OK",
                  "content": {
                    "page": 0,
                    "totalCount": 1,
                    "totalPages": 1,
                    "data": [{
                      "sessionKey": "session-1",
                      "connectedDate": "2026-07-14T00:00:00Z",
                      "disconnectedDate": null,
                      "subscribedEvents": null
                    }]
                  }
                }
                """;

        ChzzkApiResponse<SessionResponse> response = objectMapper.readValue(
                json,
                new TypeReference<ChzzkApiResponse<SessionResponse>>() {
                }
        );

        then(response.content().toSessionListResult().data()).singleElement().satisfies(session -> {
            then(session.sessionKey()).isEqualTo("session-1");
            then(session.subscribedEvents()).isNull();
        });
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

        then(response.expiresIn()).isNull();
    }

    @Test
    void authorizationDto_ToStringShouldMaskSensitiveTokens() {
        AuthorizationToken dto = new AuthorizationToken("access-secret", "refresh-secret", "Bearer", 3600, "chat");

        String result = dto.toString();

        then(result).doesNotContain("access-secret", "refresh-secret");
        then(result).contains("accessToken=<masked>", "refreshToken=<masked>");
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
}
