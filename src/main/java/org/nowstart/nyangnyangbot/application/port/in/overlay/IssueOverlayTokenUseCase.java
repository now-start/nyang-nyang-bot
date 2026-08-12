package org.nowstart.nyangnyangbot.application.port.in.overlay;

import jakarta.validation.constraints.NotBlank;

public interface IssueOverlayTokenUseCase {

    /** 현재 오버레이 토큰을 폐기하고 요청자에게 새 토큰을 발급한다. */
    OverlayTokenIssueResult issueToken(@NotBlank(message = "actorId is required") String actorId);

    record OverlayTokenIssueResult(String token) {
    }
}
