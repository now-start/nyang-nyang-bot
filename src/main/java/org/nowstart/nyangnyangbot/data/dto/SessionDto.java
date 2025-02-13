package org.nowstart.nyangnyangbot.data.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SessionDto {

    private String url;
    private int page;
    private int totalCount;
    private int totalPages;
    private SessionData data;

    @Data
    @Builder
    public static class SessionData {

        private String sessionKey;
        private String connectedDate;
        private String disconnectedDate;
        private SubscribedEvents subscribedEvents;

        @Data
        @Builder
        public static class SubscribedEvents {

            private String eventType;
            private String channelId;
        }
    }
}
