package org.nowstart.nyangnyangbot.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.data.dto.AuthorizationDto;
import org.nowstart.nyangnyangbot.data.dto.AuthorizationRequestDto;
import org.nowstart.nyangnyangbot.data.dto.ChzzkDto;
import org.nowstart.nyangnyangbot.data.dto.UserDto;
import org.nowstart.nyangnyangbot.data.entity.AuthorizationEntity;
import org.nowstart.nyangnyangbot.data.type.GrantType;
import org.nowstart.nyangnyangbot.repository.AuthorizationRepository;
import org.nowstart.nyangnyangbot.repository.ChzzkOpenApi;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AuthorizationService {

    private final ChzzkDto chzzkDto;
    private final ChzzkOpenApi chzzkOpenApi;
    private final AuthorizationRepository authorizationRepository;

    public void getAccessToken(String code, String state) {
        AuthorizationDto authorizationDto = chzzkOpenApi.getAccessToken(AuthorizationRequestDto.builder()
                .grantType(GrantType.AUTHORIZATION_CODE.getData())
                .clientId(chzzkDto.getClientId())
                .clientSecret(chzzkDto.getClientSecret())
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

    public AuthorizationEntity getAccessToken(String refreshToken) {
        AuthorizationDto authorizationDto = chzzkOpenApi.getAccessToken(AuthorizationRequestDto.builder()
                .grantType(GrantType.REFRESH_TOKEN.getData())
                .clientId(chzzkDto.getClientId())
                .clientSecret(chzzkDto.getClientSecret())
                .refreshToken(refreshToken)
                .build()).getContent();
        UserDto userDto = chzzkOpenApi.getUser(authorizationDto.getTokenType() + " " + authorizationDto.getAccessToken()).getContent();

        return authorizationRepository.save(AuthorizationEntity.builder()
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
        AuthorizationEntity authorizationEntity = authorizationRepository.findById(chzzkDto.getChannelId()).orElseThrow();
        LocalDateTime expiredDate = authorizationEntity.getModifyDate().plusSeconds(authorizationEntity.getExpiresIn());

        if (expiredDate.isBefore(LocalDateTime.now())) {
            authorizationEntity = getAccessToken(authorizationEntity.getRefreshToken());
        }

        return authorizationEntity;
    }
}
