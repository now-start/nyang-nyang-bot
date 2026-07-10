package org.nowstart.nyangnyangbot.application.service.chat;

import io.micrometer.common.util.StringUtils;
import org.nowstart.nyangnyangbot.application.port.in.chzzk.HandleChzzkEventUseCase.ChatReceived;

public final class ChatEventSupport {

    private ChatEventSupport() {
    }

    public static boolean hasSenderChannelId(ChatReceived chat) {
        return !StringUtils.isBlank(senderChannelId(chat));
    }

    public static String senderChannelId(ChatReceived chat) {
        return chat == null ? null : chat.senderChannelId();
    }

    public static String nickname(ChatReceived chat) {
        if (chat == null || chat.profile() == null || StringUtils.isBlank(chat.profile().nickname())) {
            return "";
        }
        return chat.profile().nickname();
    }

    public static String displayName(ChatReceived chat) {
        String nickname = nickname(chat);
        if (!StringUtils.isBlank(nickname)) {
            return nickname;
        }
        String senderChannelId = senderChannelId(chat);
        return senderChannelId == null ? "" : senderChannelId;
    }
}
