package org.nowstart.nyangnyangbot.application.service.chat;

import io.micrometer.common.util.StringUtils;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort.ChatEventPayload;

public final class ChatEventSupport {

    private ChatEventSupport() {
    }

    public static boolean hasSenderChannelId(ChatEventPayload chat) {
        return !StringUtils.isBlank(senderChannelId(chat));
    }

    public static String senderChannelId(ChatEventPayload chat) {
        return chat == null ? null : chat.senderChannelId();
    }

    public static String nickname(ChatEventPayload chat) {
        if (chat == null || chat.profile() == null || StringUtils.isBlank(chat.profile().nickname())) {
            return "";
        }
        return chat.profile().nickname();
    }

    public static String displayName(ChatEventPayload chat) {
        String nickname = nickname(chat);
        if (!StringUtils.isBlank(nickname)) {
            return nickname;
        }
        String senderChannelId = senderChannelId(chat);
        return senderChannelId == null ? "" : senderChannelId;
    }
}
