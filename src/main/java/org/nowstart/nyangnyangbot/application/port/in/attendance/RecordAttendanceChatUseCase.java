package org.nowstart.nyangnyangbot.application.port.in.attendance;

import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort.ChatEventPayload;

public interface RecordAttendanceChatUseCase {

    void recordChatUser(ChatEventPayload chat);
}
