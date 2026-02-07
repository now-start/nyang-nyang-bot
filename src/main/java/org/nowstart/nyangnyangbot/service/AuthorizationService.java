package org.nowstart.nyangnyangbot.service;

import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.data.dto.chzzk.AuthorizationDto;
import org.nowstart.nyangnyangbot.data.dto.chzzk.AuthorizationRequestDto;
import org.nowstart.nyangnyangbot.data.dto.chzzk.UserDto;
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

    public AuthorizationEntity getAccessToken(String code, String state) {
        AuthorizationDto authorizationDto = chzzkOpenApi.getAccessToken(new AuthorizationRequestDto(
                GrantType.AUTHORIZATION_CODE.getData(),
                chzzkProperty.clientId(),
                chzzkProperty.clientSecret(),
                code,
                state,
                null
        )).content();
        UserDto userDto = chzzkOpenApi.getUser(authorizationDto.tokenType() + " " + authorizationDto.accessToken()).content();

        return authorizationRepository.findById(userDto.channelId())
                .map(existing -> {
                    existing.setChannelName(userDto.channelName());
                    existing.setAccessToken(authorizationDto.accessToken());
                    existing.setRefreshToken(authorizationDto.refreshToken());
                    existing.setTokenType(authorizationDto.tokenType());
                    existing.setExpiresIn(authorizationDto.expiresIn());
                    existing.setScope(authorizationDto.scope());
                    return existing;
                })
                .orElseGet(() -> authorizationRepository.save(AuthorizationEntity.builder()
                        .channelId(userDto.channelId())
                        .channelName(userDto.channelName())
                        .accessToken(authorizationDto.accessToken())
                        .refreshToken(authorizationDto.refreshToken())
                        .tokenType(authorizationDto.tokenType())
                        .expiresIn(authorizationDto.expiresIn())
                        .scope(authorizationDto.scope())
                        .admin(false)
                        .build()));
    }

    public AuthorizationEntity getAccessToken() {
        AuthorizationEntity authorizationEntity = authorizationRepository.findById(chzzkProperty.channelId()).orElseThrow();

        if (authorizationEntity.getModifyDate().plusSeconds(authorizationEntity.getExpiresIn()).isBefore(LocalDateTime.now())) {
            AuthorizationDto authorizationDto = chzzkOpenApi.getAccessToken(new AuthorizationRequestDto(
                    GrantType.REFRESH_TOKEN.getData(),
                    chzzkProperty.clientId(),
                    chzzkProperty.clientSecret(),
                    null,
                    null,
                    authorizationEntity.getRefreshToken()
            )).content();
            log.info("[REFRESH_TOKEN] : {}", authorizationDto);
            UserDto userDto = chzzkOpenApi.getUser(authorizationDto.tokenType() + " " + authorizationDto.accessToken()).content();

            authorizationEntity.setChannelId(userDto.channelId());
            authorizationEntity.setChannelName(userDto.channelName());
            authorizationEntity.setAccessToken(authorizationDto.accessToken());
            authorizationEntity.setRefreshToken(authorizationDto.refreshToken());
            authorizationEntity.setTokenType(authorizationDto.tokenType());
            authorizationEntity.setExpiresIn(authorizationDto.expiresIn());
            authorizationEntity.setScope(authorizationDto.scope());
        }

        return authorizationEntity;
    }
}
