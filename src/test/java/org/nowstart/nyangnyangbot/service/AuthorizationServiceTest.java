package org.nowstart.nyangnyangbot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.data.dto.chzzk.ApiResponseDto;
import org.nowstart.nyangnyangbot.data.dto.chzzk.AuthorizationDto;
import org.nowstart.nyangnyangbot.data.dto.chzzk.AuthorizationRequestDto;
import org.nowstart.nyangnyangbot.data.dto.chzzk.UserDto;
import org.nowstart.nyangnyangbot.data.entity.AuthorizationEntity;
import org.nowstart.nyangnyangbot.data.property.ChzzkProperty;
import org.nowstart.nyangnyangbot.repository.AuthorizationRepository;
import org.nowstart.nyangnyangbot.repository.ChzzkOpenApi;

@ExtendWith(MockitoExtension.class)
class AuthorizationServiceTest {

    @Mock
    private ChzzkProperty chzzkProperty;

    @Mock
    private ChzzkOpenApi chzzkOpenApi;

    @Mock
    private AuthorizationRepository authorizationRepository;

    @Spy
    @InjectMocks
    private AuthorizationService authorizationService;

    @Test
    void getAccessToken_ShouldRefreshWhenExpiresInIsNull() {
        AuthorizationEntity entity = AuthorizationEntity.builder()
                .channelId("channel-1")
                .channelName("tester")
                .accessToken("access")
                .refreshToken("refresh")
                .tokenType("Bearer")
                .expiresIn(null)
                .scope("chat")
                .admin(false)
                .build();
        AuthorizationDto refreshedAuthorization = new AuthorizationDto("new-access", "new-refresh", "Bearer", 3600, "chat");
        UserDto refreshedUser = new UserDto("channel-1", "updated-user", "ACTIVE");

        given(chzzkProperty.channelId()).willReturn("channel-1");
        given(chzzkProperty.clientId()).willReturn("client-id");
        given(chzzkProperty.clientSecret()).willReturn("client-secret");
        given(authorizationRepository.findById("channel-1")).willReturn(Optional.of(entity));
        given(chzzkOpenApi.getAccessToken(new AuthorizationRequestDto(
                "refresh_token",
                "client-id",
                "client-secret",
                null,
                null,
                "refresh"
        ))).willReturn(new ApiResponseDto<>(200, "OK", refreshedAuthorization));
        given(chzzkOpenApi.getUser("Bearer new-access")).willReturn(new ApiResponseDto<>(200, "OK", refreshedUser));

        AuthorizationEntity result = authorizationService.getAccessToken();

        assertThat(result).isSameAs(entity);
        assertThat(result.getAccessToken()).isEqualTo("new-access");
        assertThat(result.getRefreshToken()).isEqualTo("new-refresh");
        assertThat(result.getExpiresIn()).isEqualTo(3600);
        assertThat(result.getChannelName()).isEqualTo("updated-user");
    }
}
