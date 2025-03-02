package org.nowstart.nyangnyangbot.service;

import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.data.dto.AuthorizationDto;
import org.nowstart.nyangnyangbot.data.dto.AuthorizationRequestDto;
import org.nowstart.nyangnyangbot.data.dto.UserDto;
import org.nowstart.nyangnyangbot.data.entity.AuthorizationEntity;
import org.nowstart.nyangnyangbot.data.property.ChzzkProperty;
import org.nowstart.nyangnyangbot.data.type.GrantType;
import org.nowstart.nyangnyangbot.repository.AuthorizationRepository;
import org.nowstart.nyangnyangbot.repository.ChzzkOpenApi;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AuthorizationService {

    private final ChzzkProperty chzzkProperty;
    private final ChzzkOpenApi chzzkOpenApi;
    private final AuthorizationRepository authorizationRepository;

    public void getAccessToken(String code, String state) {
        AuthorizationDto authorizationDto = chzzkOpenApi.getAccessToken(AuthorizationRequestDto.builder()
            .grantType(GrantType.AUTHORIZATION_CODE.getData())
            .clientId(chzzkProperty.getClientId())
            .clientSecret(chzzkProperty.getClientSecret())
            .code(code)
            .state(state)
            .build()).getContent();
        UserDto userDto = chzzkOpenApi.getUser(authorizationDto.getTokenType() + " " + authorizationDto.getAccessToken()).getContent();

        authorizationRepository.save(AuthorizationEntity.builder()
            .channelId(userDto.getChannelId())
            .channelName(userDto.getChannelName())
            .accessToken(authorizationDto.getAccessToken())
            .refreshToken(authorizationDto.getRefreshToken())
            .tokenType(authorizationDto.getTokenType())
            .expiresIn(authorizationDto.getExpiresIn())
            .scope(authorizationDto.getScope())
            .build());
    }

    public AuthorizationEntity getAccessToken() {
        AuthorizationEntity authorizationEntity = authorizationRepository.findById(chzzkProperty.getChannelId()).orElseThrow();

        if (authorizationEntity.getModifyDate().plusSeconds(authorizationEntity.getExpiresIn()).isBefore(LocalDateTime.now())) {
            AuthorizationDto authorizationDto = chzzkOpenApi.getAccessToken(AuthorizationRequestDto.builder()
                .grantType(GrantType.REFRESH_TOKEN.getData())
                .clientId(chzzkProperty.getClientId())
                .clientSecret(chzzkProperty.getClientSecret())
                .refreshToken(authorizationEntity.getRefreshToken())
                .build()).getContent();
            log.info("[REFRESH_TOKEN] : {}", authorizationDto);
            UserDto userDto = chzzkOpenApi.getUser(authorizationDto.getTokenType() + " " + authorizationDto.getAccessToken()).getContent();

            authorizationEntity.refreshToken(userDto, authorizationDto);
            authorizationRepository.saveAndFlush(authorizationEntity);
        }

        return authorizationEntity;
    }
}
