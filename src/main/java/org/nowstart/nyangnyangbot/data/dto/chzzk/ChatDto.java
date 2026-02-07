package org.nowstart.nyangnyangbot.data.dto.chzzk;

import java.util.List;
import java.util.Map;

public record ChatDto(
        String channelId,
        String senderChannelId,
        Profile profile,
        String content,
        Map<String, String> emojis,
        long messageTime
) {
    public record Profile(
            String nickname,
            List<Map<String, String>> badges,
            Boolean verifiedMark
    ) {
    }
}
