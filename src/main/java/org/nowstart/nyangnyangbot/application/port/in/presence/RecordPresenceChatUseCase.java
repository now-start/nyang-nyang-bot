package org.nowstart.nyangnyangbot.application.port.in.presence;

import org.nowstart.nyangnyangbot.application.port.in.chat.HandleChatEventUseCase.ChatReceived;

public interface RecordPresenceChatUseCase {

    void recordChatUser(ChatReceived chat);
}
