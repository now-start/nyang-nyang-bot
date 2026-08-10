package org.nowstart.nyangnyangbot.application.port.in.weeklychat;

import org.nowstart.nyangnyangbot.application.port.in.chat.HandleChatEventUseCase.ChatReceived;

public interface RecordWeeklyChatUseCase {

    void recordChat(ChatReceived chat);
}
