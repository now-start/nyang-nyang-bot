package org.nowstart.nyangnyangbot.data.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemDto {

    private String type;
    private SystemData data;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SystemData {

        private String sessionKey;
        private String eventType;
        private String channelId;
    }
}
