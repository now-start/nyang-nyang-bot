package org.nowstart.nyangnyangbot.application.port.in.weeklychat;

import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort.ChatEventPayload;

public interface RecordWeeklyChatUseCase {

    void recordChat(ChatEventPayload chat);
}
