package org.nowstart.nyangnyangbot.application.service.authorization;

import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.application.port.in.authorization.LoginWithChzzkUseCase;
import org.nowstart.nyangnyangbot.application.port.out.authorization.AuthorizationPort;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort.AuthorizationToken;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort.AuthorizationTokenCommand;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort.UserResult;
import org.nowstart.nyangnyangbot.config.property.ChzzkProperty;
import org.nowstart.nyangnyangbot.application.port.out.authorization.AuthorizationPort.AuthorizationAccountResult;
import org.nowstart.nyangnyangbot.domain.type.GrantType;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AuthorizationService implements LoginWithChzzkUseCase {

    private final ChzzkProperty chzzkProperty;
    private final ChzzkClientPort chzzkClientPort;
    private final AuthorizationPort authorizationPort;

    @Override
    public Result login(String code, String state) {
        AuthorizationAccountResult authorization = getAccessToken(code, state);
        return new Result(authorization.channelId(), authorization.admin());
    }

    public AuthorizationAccountResult getAccessToken(String code, String state) {
        AuthorizationToken authorizationToken = chzzkClientPort.getAccessToken(new AuthorizationTokenCommand(
                GrantType.AUTHORIZATION_CODE.getData(),
                chzzkProperty.clientId(),
                chzzkProperty.clientSecret(),
                code,
                state,
                null
        )).content();
        UserResult user = chzzkClientPort.getUser(authorizationToken.tokenType() + " " + authorizationToken.accessToken()).content();

        return authorizationPort.saveOrUpdate(user, authorizationToken);
    }

    public AuthorizationAccountResult getAccessToken() {
        AuthorizationAccountResult authorization = authorizationPort.findById(chzzkProperty.channelId()).orElseThrow();

        Integer expiresIn = authorization.expiresIn();
        LocalDateTime modifyDate = authorization.modifyDate();
        if (expiresIn == null || modifyDate == null || modifyDate.plusSeconds(expiresIn).isBefore(LocalDateTime.now())) {
            AuthorizationToken authorizationToken = chzzkClientPort.getAccessToken(new AuthorizationTokenCommand(
                    GrantType.REFRESH_TOKEN.getData(),
                    chzzkProperty.clientId(),
                    chzzkProperty.clientSecret(),
                    null,
                    null,
                    authorization.refreshToken()
            )).content();
            log.info("[REFRESH_TOKEN] token refreshed");
            UserResult user = chzzkClientPort.getUser(authorizationToken.tokenType() + " " + authorizationToken.accessToken()).content();

            authorization = authorizationPort.updateToken(chzzkProperty.channelId(), user, authorizationToken);
        }

        return authorization;
    }
}
