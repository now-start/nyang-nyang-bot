package org.nowstart.nyangnyangbot.application.dto.overlay;

public class OverlayTokenDto {

    public record IssueResponse(
            Long tokenId,
            String token
    ) {
    }
}
