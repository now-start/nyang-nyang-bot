package org.nowstart.nyangnyangbot.application.port.in.attendance;

import org.nowstart.nyangnyangbot.application.port.in.chzzk.HandleChzzkEventUseCase.ChatReceived;

public interface RecordAttendanceChatUseCase {

    void recordChatUser(ChatReceived chat);
}
