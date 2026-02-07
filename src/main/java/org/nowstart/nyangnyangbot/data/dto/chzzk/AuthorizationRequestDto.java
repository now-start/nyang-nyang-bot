package org.nowstart.nyangnyangbot.data.dto.chzzk;

public record AuthorizationRequestDto(
        String grantType,
        String clientId,
        String clientSecret,
        String code,
        String state,
        String refreshToken
) {
}
