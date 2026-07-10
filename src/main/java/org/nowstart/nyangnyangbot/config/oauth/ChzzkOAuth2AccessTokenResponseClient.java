package org.nowstart.nyangnyangbot.config.oauth;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.application.port.out.authorization.AuthorizationPort;
import org.nowstart.nyangnyangbot.application.port.out.authorization.AuthorizationPort.AuthorizationAccountResult;
import org.nowstart.nyangnyangbot.application.port.out.authorization.AuthorizationPort.SaveAuthorizationCommand;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort.AuthorizationToken;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort.AuthorizationTokenCommand;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort.UserResult;
import org.nowstart.nyangnyangbot.config.property.ChzzkProperty;
import org.nowstart.nyangnyangbot.domain.type.GrantType;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChzzkOAuth2AccessTokenResponseClient
        implements OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> {

    private final ChzzkProperty chzzkProperty;
    private final ChzzkClientPort chzzkClientPort;
    private final AuthorizationPort authorizationPort;

    @Override
    public OAuth2AccessTokenResponse getTokenResponse(OAuth2AuthorizationCodeGrantRequest authorizationGrantRequest) {
        String code = authorizationGrantRequest.getAuthorizationExchange()
                .getAuthorizationResponse()
                .getCode();
        String state = authorizationGrantRequest.getAuthorizationExchange()
                .getAuthorizationRequest()
                .getState();

        AuthorizationToken token = chzzkClientPort.getAccessToken(new AuthorizationTokenCommand(
                GrantType.AUTHORIZATION_CODE.getData(),
                chzzkProperty.clientId(),
                chzzkProperty.clientSecret(),
                code,
                state,
                null
        ));
        UserResult user = chzzkClientPort.getUser(token.tokenType() + " " + token.accessToken());
        AuthorizationAccountResult saved = authorizationPort.saveOrUpdate(saveCommand(user, token));

        OAuth2AccessTokenResponse.Builder response = OAuth2AccessTokenResponse.withToken(token.accessToken())
                .tokenType(OAuth2AccessToken.TokenType.BEARER)
                .refreshToken(token.refreshToken())
                .additionalParameters(additionalParameters(saved));
        if (token.expiresIn() != null) {
            response.expiresIn(token.expiresIn());
        }
        return response.build();
    }

    private SaveAuthorizationCommand saveCommand(UserResult user, AuthorizationToken token) {
        return new SaveAuthorizationCommand(
                user.channelId(),
                user.channelName(),
                token.accessToken(),
                token.refreshToken(),
                token.tokenType(),
                token.expiresIn(),
                token.scope()
        );
    }

    private Map<String, Object> additionalParameters(AuthorizationAccountResult saved) {
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put(ChzzkOAuth2UserService.CHANNEL_ID_PARAMETER, saved.channelId());
        parameters.put(ChzzkOAuth2UserService.CHANNEL_NAME_PARAMETER, saved.channelName());
        parameters.put(ChzzkOAuth2UserService.ADMIN_PARAMETER, saved.admin());
        return parameters;
    }
}
