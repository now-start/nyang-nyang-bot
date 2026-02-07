package org.nowstart.nyangnyangbot.data.dto.chzzk;

import java.util.List;

public record SessionDto(
        String url,
        int page,
        int totalCount,
        int totalPages,
        List<SessionData> data
) {
    public record SessionData(
            String sessionKey,
            String connectedDate,
            String disconnectedDate,
            List<SubscribedEvents> subscribedEvents
    ) {
        public record SubscribedEvents(String eventType, String channelId) {
        }
    }
}
