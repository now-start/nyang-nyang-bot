package org.nowstart.nyangnyangbot.data.dto.chzzk;

public record AuthorizationDto(
        String accessToken,
        String refreshToken,
        String tokenType,
        int expiresIn,
        String scope
) {
}
