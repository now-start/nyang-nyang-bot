package org.nowstart.nyangnyangbot.application.port.in.presence;

import org.nowstart.nyangnyangbot.application.port.in.chzzk.HandleChzzkEventUseCase.ChatReceived;

public interface RecordPresenceChatUseCase {

    void recordChatUser(ChatReceived chat);
}
