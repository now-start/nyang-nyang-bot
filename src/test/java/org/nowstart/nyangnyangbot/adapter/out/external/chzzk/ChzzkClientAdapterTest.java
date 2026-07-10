package org.nowstart.nyangnyangbot.adapter.out.external.chzzk;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.adapter.out.external.chzzk.client.ChzzkOpenApiClient;
import org.nowstart.nyangnyangbot.adapter.out.external.chzzk.request.AuthorizationRequest;
import org.nowstart.nyangnyangbot.adapter.out.external.chzzk.response.AuthorizationResponse;
import org.nowstart.nyangnyangbot.adapter.out.external.chzzk.response.ChzzkApiResponse;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort.AuthorizationTokenCommand;

@ExtendWith(MockitoExtension.class)
class ChzzkClientAdapterTest {

    @Mock
    private ChzzkOpenApiClient chzzkOpenApiClient;

    @Test
    void getAccessToken_ShouldUnwrapSuccessfulResponse() {
        ChzzkClientAdapter adapter = new ChzzkClientAdapter(chzzkOpenApiClient);
        AuthorizationTokenCommand command = new AuthorizationTokenCommand(
                "authorization_code", "client", "secret", "code", "state", null
        );
        given(chzzkOpenApiClient.getAccessToken(AuthorizationRequest.from(command))).willReturn(
                new ChzzkApiResponse<>(
                        200,
                        "OK",
                        new AuthorizationResponse("access", "refresh", "Bearer", 3600, "chat")
                )
        );

        var result = adapter.getAccessToken(command);

        then(result.accessToken()).isEqualTo("access");
    }

    @Test
    void getAccessToken_ShouldRejectErrorEnvelopeWithoutLeakingMessage() {
        ChzzkClientAdapter adapter = new ChzzkClientAdapter(chzzkOpenApiClient);
        AuthorizationTokenCommand command = new AuthorizationTokenCommand(
                "authorization_code", "client", "secret", "code", "state", null
        );
        given(chzzkOpenApiClient.getAccessToken(AuthorizationRequest.from(command))).willReturn(
                new ChzzkApiResponse<>(401, "sensitive upstream detail", null)
        );

        thenThrownBy(() -> adapter.getAccessToken(command))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("CHZZK API request failed: operation=getAccessToken, code=401")
                .hasMessageNotContaining("sensitive upstream detail");
    }
}
