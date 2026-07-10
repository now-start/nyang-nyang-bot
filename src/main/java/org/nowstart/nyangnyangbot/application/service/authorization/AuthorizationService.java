package org.nowstart.nyangnyangbot.application.service.authorization;

import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.application.port.in.authorization.GetAuthorizationAccessTokenUseCase;
import org.nowstart.nyangnyangbot.application.port.out.authorization.AuthorizationPort;
import org.nowstart.nyangnyangbot.application.port.out.authorization.AuthorizationPort.AuthorizationAccountResult;
import org.nowstart.nyangnyangbot.application.port.out.authorization.AuthorizationPort.SaveAuthorizationCommand;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkConfigurationPort;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort.AuthorizationToken;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort.AuthorizationTokenCommand;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort.UserResult;
import org.nowstart.nyangnyangbot.domain.type.GrantType;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AuthorizationService implements GetAuthorizationAccessTokenUseCase {

    private final ChzzkConfigurationPort chzzkConfigurationPort;
    private final ChzzkClientPort chzzkClientPort;
    private final AuthorizationPort authorizationPort;

    @Override
    public AuthorizationAccountResult getAccessToken() {
        AuthorizationAccountResult authorization = authorizationPort.findById(chzzkConfigurationPort.channelId())
                .orElseThrow();

        Integer expiresIn = authorization.expiresIn();
        LocalDateTime modifyDate = authorization.modifyDate();
        if (expiresIn == null || modifyDate == null || modifyDate.plusSeconds(expiresIn).isBefore(LocalDateTime.now())) {
            AuthorizationToken authorizationToken = chzzkClientPort.getAccessToken(new AuthorizationTokenCommand(
                    GrantType.REFRESH_TOKEN.getData(),
                    chzzkConfigurationPort.clientId(),
                    chzzkConfigurationPort.clientSecret(),
                    null,
                    null,
                    authorization.refreshToken()
            ));
            log.info("[REFRESH_TOKEN] token refreshed");
            UserResult user = chzzkClientPort.getUser(
                    authorizationToken.tokenType() + " " + authorizationToken.accessToken()
            );

            authorization = authorizationPort.updateToken(
                    chzzkConfigurationPort.channelId(),
                    saveCommand(user, authorizationToken)
            );
        }

        return authorization;
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
}
