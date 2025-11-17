package org.nowstart.nyangnyangbot.data.entity;

import static org.assertj.core.api.BDDAssertions.then;

import org.junit.jupiter.api.Test;

class AuthorizationEntityTest {

    @Test
    void authorizationEntity_ShouldBuildCorrectly() {
        // when
        AuthorizationEntity entity = AuthorizationEntity.builder()
                .channelId("channel123")
                .channelName("테스트채널")
                .accessToken("accessToken123")
                .refreshToken("refreshToken123")
                .tokenType("Bearer")
                .expiresIn(3600)
                .scope("chat:read chat:write")
                .build();

        // then
        then(entity.getChannelId()).isEqualTo("channel123");
        then(entity.getChannelName()).isEqualTo("테스트채널");
        then(entity.getAccessToken()).isEqualTo("accessToken123");
        then(entity.getRefreshToken()).isEqualTo("refreshToken123");
        then(entity.getTokenType()).isEqualTo("Bearer");
        then(entity.getExpiresIn()).isEqualTo(3600);
        then(entity.getScope()).isEqualTo("chat:read chat:write");
    }

    @Test
    void authorizationEntity_ShouldSupportSetters() {
        // given
        AuthorizationEntity entity = new AuthorizationEntity();

        // when
        entity.setChannelId("newChannel");
        entity.setAccessToken("newToken");
        entity.setExpiresIn(7200);

        // then
        then(entity.getChannelId()).isEqualTo("newChannel");
        then(entity.getAccessToken()).isEqualTo("newToken");
        then(entity.getExpiresIn()).isEqualTo(7200);
    }

    @Test
    void authorizationEntity_ShouldHandleNullValues() {
        // when
        AuthorizationEntity entity = AuthorizationEntity.builder()
                .channelId("channel123")
                .build();

        // then
        then(entity.getChannelId()).isEqualTo("channel123");
        then(entity.getAccessToken()).isNull();
        then(entity.getRefreshToken()).isNull();
    }
}
