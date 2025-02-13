package org.nowstart.nyangnyangbot.data.dto;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatDto {

    private String channelId;
    private String senderChannelId;
    private Profile profile;
    private String content;
    private Map<String, String> emojis;
    private long messageTime;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Profile {

        private String nickname;
        private List<Map<String, String>> badges;
        private Boolean verifiedMark;
    }
}
