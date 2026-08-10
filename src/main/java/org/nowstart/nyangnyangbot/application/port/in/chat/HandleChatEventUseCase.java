package org.nowstart.nyangnyangbot.application.port.in.chat;

import java.util.List;
import java.util.Map;

public interface HandleChatEventUseCase {

    void handle(ChatReceived event);

    record ChatReceived(
            String channelId,
            String senderChannelId,
            Profile profile,
            String content,
            Map<String, String> emojis,
            Long messageTime
    ) {

        public record Profile(
                String nickname,
                List<Map<String, String>> badges,
                Boolean verifiedMark
        ) {
        }
    }
}
