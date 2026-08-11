package org.nowstart.nyangnyangbot.application.port.in.chat;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

public interface HandleChatEventUseCase {

    void handle(@Valid @NotNull(message = "chat is required") ChatReceived event);

    record ChatReceived(
            @NotBlank(message = "channelId is required") String channelId,
            String senderChannelId,
            @Valid Profile profile,
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
