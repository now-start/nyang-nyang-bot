package org.nowstart.nyangnyangbot.service;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.data.dto.ApiResponseDto;
import org.nowstart.nyangnyangbot.data.dto.AuthorizationDto;
import org.nowstart.nyangnyangbot.data.dto.AuthorizationRequestDto;
import org.nowstart.nyangnyangbot.data.dto.UserDto;
import org.nowstart.nyangnyangbot.data.entity.AuthorizationEntity;
import org.nowstart.nyangnyangbot.data.property.ChzzkProperty;
import org.nowstart.nyangnyangbot.data.type.GrantType;
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

    @InjectMocks
    private AuthorizationService authorizationService;

    private AuthorizationDto authorizationDto;
    private UserDto userDto;
    private AuthorizationEntity authorizationEntity;

    @BeforeEach
    void setUp() {
        authorizationDto = AuthorizationDto.builder()
                .accessToken("newAccessToken")
                .refreshToken("newRefreshToken")
                .tokenType("Bearer")
                .expiresIn(3600)
                .scope("chat:read chat:write")
                .build();

        userDto = UserDto.builder()
                .channelId("channel123")
                .channelName("testChannel")
                .build();

        authorizationEntity = AuthorizationEntity.builder()
                .channelId("channel123")
                .channelName("testChannel")
                .accessToken("oldAccessToken")
                .refreshToken("oldRefreshToken")
                .tokenType("Bearer")
                .expiresIn(3600)
                .scope("chat:read chat:write")
                .build();
    }

    @Test
    void getAccessToken_WithCodeAndState_ShouldSaveNewAuthorization() {
        // given
        String code = "authCode123";
        String state = "stateValue";

        ApiResponseDto<AuthorizationDto> authResponse = ApiResponseDto.<AuthorizationDto>builder().build();
        authResponse.setContent(authorizationDto);

        ApiResponseDto<UserDto> userResponse = ApiResponseDto.<UserDto>builder().build();
        userResponse.setContent(userDto);

        given(chzzkOpenApi.getAccessToken(any(AuthorizationRequestDto.class))).willReturn(authResponse);
        given(chzzkOpenApi.getUser("Bearer newAccessToken")).willReturn(userResponse);
        given(authorizationRepository.findById("channel123")).willReturn(Optional.empty());
        given(authorizationRepository.save(any(AuthorizationEntity.class))).willReturn(authorizationEntity);

        // when
        AuthorizationEntity result = authorizationService.getAccessToken(code, state);

        // then
        then(result).isNotNull();
        ArgumentCaptor<AuthorizationRequestDto> requestCaptor = ArgumentCaptor.forClass(AuthorizationRequestDto.class);
        BDDMockito.then(chzzkOpenApi).should().getAccessToken(requestCaptor.capture());

        AuthorizationRequestDto capturedRequest = requestCaptor.getValue();
        then(capturedRequest.getGrantType()).isEqualTo(GrantType.AUTHORIZATION_CODE.getData());
        then(capturedRequest.getCode()).isEqualTo(code);
        then(capturedRequest.getState()).isEqualTo(state);

        ArgumentCaptor<AuthorizationEntity> entityCaptor = ArgumentCaptor.forClass(AuthorizationEntity.class);
        BDDMockito.then(authorizationRepository).should().save(entityCaptor.capture());

        AuthorizationEntity savedEntity = entityCaptor.getValue();
        then(savedEntity.getChannelId()).isEqualTo("channel123");
        then(savedEntity.getAccessToken()).isEqualTo("newAccessToken");
    }

    @Test
    void getAccessToken_NoArgs_ShouldReturnExistingToken_WhenNotExpired() {
        // given
        authorizationEntity.setAccessToken("validToken");
        given(authorizationRepository.findById("channel123")).willReturn(Optional.of(authorizationEntity));

        // when
        AuthorizationEntity result = authorizationService.getAccessToken();

        // then
        then(result).isNotNull();
        then(result.getAccessToken()).isEqualTo("validToken");
        BDDMockito.then(chzzkOpenApi).should(never()).getAccessToken(any());
    }

    @Test
    void getAccessToken_NoArgs_ShouldRefreshToken_WhenExpired() {
        // given
        authorizationEntity.setAccessToken("expiredToken");

        ApiResponseDto<AuthorizationDto> authResponse = ApiResponseDto.<AuthorizationDto>builder().build();
        authResponse.setContent(authorizationDto);

        ApiResponseDto<UserDto> userResponse = ApiResponseDto.<UserDto>builder().build();
        userResponse.setContent(userDto);

        given(authorizationRepository.findById("channel123")).willReturn(Optional.of(authorizationEntity));
        given(chzzkOpenApi.getAccessToken(any(AuthorizationRequestDto.class))).willReturn(authResponse);
        given(chzzkOpenApi.getUser("Bearer newAccessToken")).willReturn(userResponse);

        // when
        AuthorizationEntity result = authorizationService.getAccessToken();

        // then
        then(result).isNotNull();
        then(result.getAccessToken()).isEqualTo("newAccessToken");
        then(result.getRefreshToken()).isEqualTo("newRefreshToken");

        ArgumentCaptor<AuthorizationRequestDto> requestCaptor = ArgumentCaptor.forClass(AuthorizationRequestDto.class);
        BDDMockito.then(chzzkOpenApi).should().getAccessToken(requestCaptor.capture());

        AuthorizationRequestDto capturedRequest = requestCaptor.getValue();
        then(capturedRequest.getGrantType()).isEqualTo(GrantType.REFRESH_TOKEN.getData());
        then(capturedRequest.getRefreshToken()).isEqualTo("oldRefreshToken");
    }

    @Test
    void getAccessToken_NoArgs_ShouldThrowException_WhenNotFound() {
        // given
        given(authorizationRepository.findById("channel123")).willReturn(Optional.empty());

        // when & then
        thenThrownBy(() -> authorizationService.getAccessToken()).isInstanceOf(Exception.class);
    }

    @Test
    void getAccessToken_WithCodeAndState_ShouldCallApiWithCorrectParameters() {
        // given
        String code = "code123";
        String state = "state456";

        ApiResponseDto<AuthorizationDto> authResponse = ApiResponseDto.<AuthorizationDto>builder().build();
        authResponse.setContent(authorizationDto);

        ApiResponseDto<UserDto> userResponse = ApiResponseDto.<UserDto>builder().build();
        userResponse.setContent(userDto);

        given(chzzkOpenApi.getAccessToken(any())).willReturn(authResponse);
        given(chzzkOpenApi.getUser(anyString())).willReturn(userResponse);
        given(authorizationRepository.findById(anyString())).willReturn(Optional.empty());
        given(authorizationRepository.save(any(AuthorizationEntity.class))).willReturn(authorizationEntity);

        // when
        AuthorizationEntity result = authorizationService.getAccessToken(code, state);

        // then
        then(result).isNotNull();
        BDDMockito.then(chzzkOpenApi).should().getAccessToken(any(AuthorizationRequestDto.class));
        BDDMockito.then(chzzkOpenApi).should().getUser("Bearer newAccessToken");
        BDDMockito.then(authorizationRepository).should().save(any(AuthorizationEntity.class));
    }

    @Test
    void getAccessToken_NoArgs_ShouldUpdateAllFields_WhenRefreshing() {
        // given
        authorizationEntity.setAccessToken("oldToken");

        AuthorizationDto newAuthDto = AuthorizationDto.builder()
                .accessToken("freshAccessToken")
                .refreshToken("freshRefreshToken")
                .tokenType("Bearer")
                .expiresIn(7200)
                .scope("updated:scope")
                .build();

        UserDto newUserDto = UserDto.builder()
                .channelId("newChannelId")
                .channelName("newChannelName")
                .build();

        ApiResponseDto<AuthorizationDto> authResponse = ApiResponseDto.<AuthorizationDto>builder().build();
        authResponse.setContent(newAuthDto);

        ApiResponseDto<UserDto> userResponse = ApiResponseDto.<UserDto>builder().build();
        userResponse.setContent(newUserDto);

        given(authorizationRepository.findById("channel123")).willReturn(Optional.of(authorizationEntity));
        given(chzzkOpenApi.getAccessToken(any())).willReturn(authResponse);
        given(chzzkOpenApi.getUser(anyString())).willReturn(userResponse);

        // when
        AuthorizationEntity result = authorizationService.getAccessToken();

        // then
        then(result.getChannelId()).isEqualTo("newChannelId");
        then(result.getChannelName()).isEqualTo("newChannelName");
        then(result.getAccessToken()).isEqualTo("freshAccessToken");
        then(result.getRefreshToken()).isEqualTo("freshRefreshToken");
        then(result.getExpiresIn()).isEqualTo(7200);
        then(result.getScope()).isEqualTo("updated:scope");
    }
}
