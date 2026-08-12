package org.nowstart.nyangnyangbot.adapter.out.external.chzzk;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.nowstart.nyangnyangbot.support.MethodValidationTestSupport.validated;

import jakarta.validation.ConstraintViolationException;
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
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort;
import org.nowstart.nyangnyangbot.application.exception.ExternalSystemException;

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
                .isInstanceOf(ExternalSystemException.class)
                .hasMessage("CHZZK API request failed: operation=getAccessToken, code=401")
                .hasMessageNotContaining("sensitive upstream detail");
    }

    @Test
    void getAccessToken_ShouldWrapTransportFailure() {
        AuthorizationTokenCommand command = new AuthorizationTokenCommand(
                "authorization_code", "client", "secret", "code", "state", null
        );
        RuntimeException transportFailure = new RuntimeException("sensitive transport detail");
        given(chzzkOpenApiClient.getAccessToken(AuthorizationRequest.from(command))).willThrow(transportFailure);

        thenThrownBy(() -> adapter().getAccessToken(command))
                .isInstanceOf(ExternalSystemException.class)
                .hasMessage("CHZZK API request failed: operation=getAccessToken")
                .hasCause(transportFailure)
                .hasMessageNotContaining("sensitive transport detail");
    }

    @Test
    void getAccessToken_ShouldRejectInvalidRequestBeforeExternalCall() {
        AuthorizationTokenCommand command = new AuthorizationTokenCommand(null, null, null, null, null, null);
        ChzzkClientPort validatedAdapter = validated(adapter(), ChzzkClientPort.class);

        thenThrownBy(() -> validatedAdapter.getAccessToken(command))
                .isInstanceOf(ConstraintViolationException.class)
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

        ChzzkClientPort validatedAdapter = validated(adapter(), ChzzkClientPort.class);

        thenThrownBy(() -> validatedAdapter.getAccessToken(command))
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("accessToken is required")
                .hasMessageContaining("refreshToken is required");
    }

    @Test
    void getSession_ShouldAcceptUrlOnlyResponse() {
        given(chzzkOpenApiClient.getSession("client", "secret")).willReturn(
                new ChzzkApiResponse<>(200, "OK", new SessionResponse("wss://example", null, null, null, null))
        );

        var result = adapter().getSession("client", "secret");

        then(result.url()).isEqualTo("wss://example");
    }

    @Test
    void getSession_ShouldRejectMissingUrl() {
        given(chzzkOpenApiClient.getSession("client", "secret")).willReturn(
                new ChzzkApiResponse<>(200, "OK", new SessionResponse(null, null, null, null, null))
        );

        ChzzkClientPort validatedAdapter = validated(adapter(), ChzzkClientPort.class);

        thenThrownBy(() -> validatedAdapter.getSession("client", "secret"))
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("url is required");
    }

    @Test
    void getSessionList_ShouldRejectMissingSessionData() {
        given(chzzkOpenApiClient.getSessionList("client", "secret")).willReturn(
                new ChzzkApiResponse<>(200, "OK", new SessionResponse(null, 0, 0, 0, null))
        );

        ChzzkClientPort validatedAdapter = validated(adapter(), ChzzkClientPort.class);

        thenThrownBy(() -> validatedAdapter.getSessionList("client", "secret"))
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("data is required");
    }

    @Test
    void getSessionList_ShouldMapSessionData() {
        SessionResponse.SessionData session = new SessionResponse.SessionData(
                "session-1",
                "2026-07-14T00:00:00Z",
                null,
                List.of(new SessionResponse.SessionData.SubscribedEvents("CHAT", "channel-1"))
        );
        given(chzzkOpenApiClient.getSessionList("client", "secret")).willReturn(
                new ChzzkApiResponse<>(200, "OK", new SessionResponse(null, 0, 1, 1, List.of(session)))
        );

        var result = adapter().getSessionList("client", "secret");

        then(result.data()).singleElement().satisfies(sessionResult -> {
            then(sessionResult.sessionKey()).isEqualTo("session-1");
            then(sessionResult.subscribedEvents()).singleElement().satisfies(event -> {
                then(event.eventType()).isEqualTo("CHAT");
                then(event.channelId()).isEqualTo("channel-1");
            });
        });
    }

    @Test
    void getSessionList_ShouldRejectMissingSubscribedEvents() {
        SessionResponse.SessionData session = new SessionResponse.SessionData(
                "session-1", null, null, null
        );
        given(chzzkOpenApiClient.getSessionList("client", "secret")).willReturn(
                new ChzzkApiResponse<>(200, "OK", new SessionResponse(null, 0, 1, 1, List.of(session)))
        );

        ChzzkClientPort validatedAdapter = validated(adapter(), ChzzkClientPort.class);

        thenThrownBy(() -> validatedAdapter.getSessionList("client", "secret"))
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("subscribedEvents is required");
    }

    private ChzzkClientAdapter adapter() {
        return new ChzzkClientAdapter(chzzkOpenApiClient);
    }
}
