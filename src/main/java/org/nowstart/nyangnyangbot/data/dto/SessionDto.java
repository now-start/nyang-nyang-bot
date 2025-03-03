package org.nowstart.nyangnyangbot.data.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SessionDto {

    private String url;
    private int page;
    private int totalCount;
    private int totalPages;
    private List<SessionData> data;

    @Data
    @Builder
    public static class SessionData {

        private String sessionKey;
        private String connectedDate;
        private String disconnectedDate;
        private List<SubscribedEvents> subscribedEvents;

        @Data
        @Builder
        public static class SubscribedEvents {

            private String eventType;
            private String channelId;
        }
    }
}
