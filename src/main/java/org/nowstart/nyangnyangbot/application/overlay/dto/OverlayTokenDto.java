package org.nowstart.nyangnyangbot.application.overlay.dto;

public class OverlayTokenDto {

    public record IssueResponse(
            Long tokenId,
            String token
    ) {
    }
}
