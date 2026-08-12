package org.nowstart.nyangnyangbot.application.port.in.overlay;

public interface ValidateOverlayTokenUseCase {

    /** 원본 토큰이 활성 오버레이 토큰과 일치하는지 반환한다. */
    boolean validateToken(String rawToken);
}
