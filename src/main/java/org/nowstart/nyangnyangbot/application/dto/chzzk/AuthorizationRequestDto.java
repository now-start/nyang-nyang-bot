package org.nowstart.nyangnyangbot.application.dto.chzzk;

public record AuthorizationRequestDto(
        String grantType,
        String clientId,
        String clientSecret,
        String code,
        String state,
        String refreshToken
) {

    @Override
    public String toString() {
        return "AuthorizationRequestDto[grantType=%s, clientId=%s, clientSecret=<masked>, code=<masked>, state=<masked>, refreshToken=<masked>]"
                .formatted(grantType, clientId);
    }
}
