package org.nowstart.nyangnyangbot.application.service;

import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.domain.model.AuthorizationAccount;
import org.nowstart.nyangnyangbot.application.gateway.out.authorization.AuthorizationPort;
import org.nowstart.nyangnyangbot.application.gateway.out.chzzk.ChzzkClientPort;
import org.nowstart.nyangnyangbot.application.dto.chzzk.AuthorizationDto;
import org.nowstart.nyangnyangbot.application.dto.chzzk.AuthorizationRequestDto;
import org.nowstart.nyangnyangbot.application.dto.chzzk.UserDto;
import org.nowstart.nyangnyangbot.config.property.ChzzkProperty;
import org.nowstart.nyangnyangbot.domain.type.GrantType;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AuthorizationService {

    private final ChzzkProperty chzzkProperty;
    private final ChzzkClientPort chzzkClientPort;
    private final AuthorizationPort authorizationPort;

    public AuthorizationAccount getAccessToken(String code, String state) {
        AuthorizationDto authorizationDto = chzzkClientPort.getAccessToken(new AuthorizationRequestDto(
                GrantType.AUTHORIZATION_CODE.getData(),
                chzzkProperty.clientId(),
                chzzkProperty.clientSecret(),
                code,
                state,
                null
        )).content();
        UserDto userDto = chzzkClientPort.getUser(authorizationDto.tokenType() + " " + authorizationDto.accessToken()).content();

        return authorizationPort.saveOrUpdate(userDto, authorizationDto);
    }

    public AuthorizationAccount getAccessToken() {
        AuthorizationAccount authorization = authorizationPort.findById(chzzkProperty.channelId()).orElseThrow();

        Integer expiresIn = authorization.expiresIn();
        LocalDateTime modifyDate = authorization.modifyDate();
        if (expiresIn == null || modifyDate == null || modifyDate.plusSeconds(expiresIn).isBefore(LocalDateTime.now())) {
            AuthorizationDto authorizationDto = chzzkClientPort.getAccessToken(new AuthorizationRequestDto(
                    GrantType.REFRESH_TOKEN.getData(),
                    chzzkProperty.clientId(),
                    chzzkProperty.clientSecret(),
                    null,
                    null,
                    authorization.refreshToken()
            )).content();
            log.info("[REFRESH_TOKEN] token refreshed");
            UserDto userDto = chzzkClientPort.getUser(authorizationDto.tokenType() + " " + authorizationDto.accessToken()).content();

            authorization = authorizationPort.updateToken(chzzkProperty.channelId(), userDto, authorizationDto);
        }

        return authorization;
    }
}
