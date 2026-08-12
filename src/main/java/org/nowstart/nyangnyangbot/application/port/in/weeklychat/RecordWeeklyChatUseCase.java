package org.nowstart.nyangnyangbot.application.port.in.weeklychat;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.nowstart.nyangnyangbot.application.port.in.chat.HandleChatEventUseCase.ChatReceived;

public interface RecordWeeklyChatUseCase {

    /** 현재 주간 집계 구간에 채팅 발신자의 활동을 기록한다. */
    void recordChat(@Valid @NotNull(message = "chat is required") ChatReceived chat);
}
