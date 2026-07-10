package org.nowstart.nyangnyangbot.application.port.in.weeklychat;

import org.nowstart.nyangnyangbot.application.port.in.chzzk.HandleChzzkEventUseCase.ChatReceived;

public interface RecordWeeklyChatUseCase {

    void recordChat(ChatReceived chat);
}
