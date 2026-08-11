package org.nowstart.nyangnyangbot.application.port.in.overlay;

import jakarta.validation.constraints.NotBlank;

public interface IssueOverlayTokenUseCase {

    OverlayTokenIssueResult issueToken(@NotBlank(message = "actorId is required") String actorId);

    record OverlayTokenIssueResult(String token) {
    }
}
