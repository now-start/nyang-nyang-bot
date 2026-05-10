package org.nowstart.nyangnyangbot.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.domain.model.AuthorizationAccount;
import org.nowstart.nyangnyangbot.application.gateway.out.authorization.AuthorizationPort;
import org.nowstart.nyangnyangbot.application.gateway.out.chzzk.ChzzkClientPort;
import org.nowstart.nyangnyangbot.application.dto.chzzk.ApiResponseDto;
import org.nowstart.nyangnyangbot.application.dto.chzzk.AuthorizationDto;
import org.nowstart.nyangnyangbot.application.dto.chzzk.AuthorizationRequestDto;
import org.nowstart.nyangnyangbot.application.dto.chzzk.UserDto;
import org.nowstart.nyangnyangbot.config.property.ChzzkProperty;

@ExtendWith(MockitoExtension.class)
class AuthorizationServiceTest {

    @Mock
    private ChzzkProperty chzzkProperty;

    @Mock
    private ChzzkClientPort chzzkClientPort;

    @Mock
    private AuthorizationPort authorizationPort;

    @Spy
    @InjectMocks
    private AuthorizationService authorizationService;

    @Test
    void getAccessToken_ShouldRefreshWhenExpiresInIsNull() {
        AuthorizationAccount account = new AuthorizationAccount(
                "channel-1", "tester", "access", "refresh", "Bearer", null, "chat", false, null, null
        );
        AuthorizationDto refreshedAuthorization = new AuthorizationDto("new-access", "new-refresh", "Bearer", 3600, "chat");
        UserDto refreshedUser = new UserDto("channel-1", "updated-user", "ACTIVE");
        AuthorizationAccount refreshed = new AuthorizationAccount(
                "channel-1", "updated-user", "new-access", "new-refresh", "Bearer", 3600, "chat", false, null, null
        );

        given(chzzkProperty.channelId()).willReturn("channel-1");
        given(chzzkProperty.clientId()).willReturn("client-id");
        given(chzzkProperty.clientSecret()).willReturn("client-secret");
        given(authorizationPort.findById("channel-1")).willReturn(Optional.of(account));
        given(chzzkClientPort.getAccessToken(new AuthorizationRequestDto(
                "refresh_token",
                "client-id",
                "client-secret",
                null,
                null,
                "refresh"
        ))).willReturn(new ApiResponseDto<>(200, "OK", refreshedAuthorization));
        given(chzzkClientPort.getUser("Bearer new-access")).willReturn(new ApiResponseDto<>(200, "OK", refreshedUser));
        given(authorizationPort.updateToken("channel-1", refreshedUser, refreshedAuthorization)).willReturn(refreshed);

        AuthorizationAccount result = authorizationService.getAccessToken();

        assertThat(result.accessToken()).isEqualTo("new-access");
        assertThat(result.refreshToken()).isEqualTo("new-refresh");
        assertThat(result.expiresIn()).isEqualTo(3600);
        assertThat(result.channelName()).isEqualTo("updated-user");
    }
}
