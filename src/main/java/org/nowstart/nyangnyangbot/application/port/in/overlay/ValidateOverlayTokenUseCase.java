package org.nowstart.nyangnyangbot.application.port.in.overlay;

public interface ValidateOverlayTokenUseCase {

    boolean validateToken(String rawToken);
}
