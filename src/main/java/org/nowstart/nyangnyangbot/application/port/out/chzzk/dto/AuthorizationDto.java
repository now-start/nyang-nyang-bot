package org.nowstart.nyangnyangbot.application.port.out.chzzk.dto;

public record AuthorizationDto(
        String accessToken,
        String refreshToken,
        String tokenType,
        Integer expiresIn,
        String scope
) {

    @Override
    public String toString() {
        return "AuthorizationDto[accessToken=<masked>, refreshToken=<masked>, tokenType=%s, expiresIn=%s, scope=%s]"
                .formatted(tokenType, expiresIn, scope);
    }
}
