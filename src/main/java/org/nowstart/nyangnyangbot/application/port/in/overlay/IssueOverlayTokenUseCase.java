package org.nowstart.nyangnyangbot.application.port.in.overlay;

public interface IssueOverlayTokenUseCase {

    OverlayTokenIssueResult issueToken(String actorId);

    record OverlayTokenIssueResult(String token) {
    }
}
