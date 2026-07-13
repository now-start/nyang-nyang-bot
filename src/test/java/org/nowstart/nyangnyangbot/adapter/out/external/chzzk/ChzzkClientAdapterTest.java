package org.nowstart.nyangnyangbot.adapter.out.external.chzzk;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;

import jakarta.validation.Validation;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.adapter.out.external.chzzk.client.ChzzkOpenApiClient;
import org.nowstart.nyangnyangbot.adapter.out.external.chzzk.request.AuthorizationRequest;
import org.nowstart.nyangnyangbot.adapter.out.external.chzzk.response.AuthorizationResponse;
import org.nowstart.nyangnyangbot.adapter.out.external.chzzk.response.ChzzkApiResponse;
import org.nowstart.nyangnyangbot.adapter.out.external.chzzk.response.SessionResponse;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort.AuthorizationTokenCommand;
import org.nowstart.nyangnyangbot.application.validation.outbound.ExternalResponseContractException;
import org.nowstart.nyangnyangbot.application.validation.outbound.OutboundRequestContractException;
import org.nowstart.nyangnyangbot.adapter.out.validation.OutboundContractValidator;

@ExtendWith(MockitoExtension.class)
class ChzzkClientAdapterTest {

    @Mock
    private ChzzkOpenApiClient chzzkOpenApiClient;

    @Test
    void getAccessToken_ShouldUnwrapSuccessfulResponse() {
        ChzzkClientAdapter adapter = adapter();
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
        ChzzkClientAdapter adapter = adapter();
        AuthorizationTokenCommand command = new AuthorizationTokenCommand(
                "authorization_code", "client", "secret", "code", "state", null
        );
        given(chzzkOpenApiClient.getAccessToken(AuthorizationRequest.from(command))).willReturn(
                new ChzzkApiResponse<>(401, "sensitive upstream detail", null)
        );

        thenThrownBy(() -> adapter.getAccessToken(command))
                .isInstanceOf(ExternalResponseContractException.class)
                .hasMessage("CHZZK API request failed: operation=getAccessToken, code=401")
                .hasMessageNotContaining("sensitive upstream detail");
    }

    @Test
    void getAccessToken_ShouldRejectInvalidRequestBeforeExternalCall() {
        AuthorizationTokenCommand command = new AuthorizationTokenCommand(null, null, null, null, null, null);

        thenThrownBy(() -> adapter().getAccessToken(command))
                .isInstanceOf(OutboundRequestContractException.class)
                .hasMessageContaining("grantType is required");
        org.mockito.BDDMockito.then(chzzkOpenApiClient).should(never()).getAccessToken(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void getAccessToken_ShouldRejectInvalidSuccessfulResponse() {
        AuthorizationTokenCommand command = new AuthorizationTokenCommand(
                "authorization_code", "client", "secret", "code", "state", null
        );
        given(chzzkOpenApiClient.getAccessToken(AuthorizationRequest.from(command))).willReturn(
                new ChzzkApiResponse<>(200, "OK", new AuthorizationResponse(null, null, "Bearer", 3600, "chat"))
        );

        thenThrownBy(() -> adapter().getAccessToken(command))
                .isInstanceOf(ExternalResponseContractException.class)
                .hasMessageContaining("accessToken is required")
                .hasMessageContaining("refreshToken is required");
    }

    @Test
    void getSession_ShouldRejectMissingSessionData() {
        given(chzzkOpenApiClient.getSession("client", "secret")).willReturn(
                new ChzzkApiResponse<>(200, "OK", new SessionResponse(null, 0, 0, 0, null))
        );

        thenThrownBy(() -> adapter().getSession("client", "secret"))
                .isInstanceOf(ExternalResponseContractException.class)
                .hasMessageContaining("data is required");
    }

    @Test
    void getSession_ShouldRejectMissingSubscribedEvents() {
        SessionResponse.SessionData session = new SessionResponse.SessionData(
                "session-1", null, null, null
        );
        given(chzzkOpenApiClient.getSession("client", "secret")).willReturn(
                new ChzzkApiResponse<>(200, "OK", new SessionResponse(null, 0, 1, 1, List.of(session)))
        );

        thenThrownBy(() -> adapter().getSession("client", "secret"))
                .isInstanceOf(ExternalResponseContractException.class)
                .hasMessageContaining("subscribedEvents is required");
    }

    private ChzzkClientAdapter adapter() {
        return new ChzzkClientAdapter(
                chzzkOpenApiClient,
                new OutboundContractValidator(Validation.buildDefaultValidatorFactory().getValidator())
        );
    }
}
