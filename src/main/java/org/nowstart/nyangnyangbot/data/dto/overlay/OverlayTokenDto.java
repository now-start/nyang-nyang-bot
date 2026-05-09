package org.nowstart.nyangnyangbot.data.dto.overlay;

public class OverlayTokenDto {

    public record IssueResponse(
            Long tokenId,
            String token
    ) {
    }
}
