package org.nowstart.nyangnyangbot.service;

import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.data.dto.chzzk.AuthorizationDto;
import org.nowstart.nyangnyangbot.data.dto.chzzk.AuthorizationRequestDto;
import org.nowstart.nyangnyangbot.data.dto.chzzk.UserDto;
import org.nowstart.nyangnyangbot.data.entity.AuthorizationEntity;
import org.nowstart.nyangnyangbot.data.entity.ChannelEntity;
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
    private final ChannelService channelService;

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

        ChannelEntity channel = channelService.getOrCreate(userDto.channelId(), userDto.channelName());

        return authorizationRepository.findByChannelId(channel.getId())
                .map(existing -> {
                    existing.setAccessToken(authorizationDto.accessToken());
                    existing.setRefreshToken(authorizationDto.refreshToken());
                    existing.setTokenType(authorizationDto.tokenType());
                    existing.setExpiresIn(authorizationDto.expiresIn());
                    existing.setScope(authorizationDto.scope());
                    return existing;
                })
                .orElseGet(() -> authorizationRepository.save(AuthorizationEntity.builder()
                        .channel(channel)
                        .accessToken(authorizationDto.accessToken())
                        .refreshToken(authorizationDto.refreshToken())
                        .tokenType(authorizationDto.tokenType())
                        .expiresIn(authorizationDto.expiresIn())
                        .scope(authorizationDto.scope())
                        .admin(false)
                        .build()));
    }

    public AuthorizationEntity getAccessToken() {
        ChannelEntity channel = channelService.getOrCreate(chzzkProperty.channelId(), null);
        AuthorizationEntity authorizationEntity = authorizationRepository.findByChannelId(channel.getId()).orElseThrow();

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

            channelService.getOrCreate(userDto.channelId(), userDto.channelName());
            authorizationEntity.setAccessToken(authorizationDto.accessToken());
            authorizationEntity.setRefreshToken(authorizationDto.refreshToken());
            authorizationEntity.setTokenType(authorizationDto.tokenType());
            authorizationEntity.setExpiresIn(authorizationDto.expiresIn());
            authorizationEntity.setScope(authorizationDto.scope());
        }

        return authorizationEntity;
    }
}
