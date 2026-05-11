package org.nowstart.nyangnyangbot.adapter.in.web.overlay.response;

import org.nowstart.nyangnyangbot.application.port.in.overlay.dto.OverlayTokenIssueResult;

public record OverlayTokenIssueResponse(
        Long tokenId,
        String token
) {

    public static OverlayTokenIssueResponse from(OverlayTokenIssueResult result) {
        return new OverlayTokenIssueResponse(result.tokenId(), result.token());
    }
}
