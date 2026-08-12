package org.nowstart.nyangnyangbot.application.port.in.presence;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.nowstart.nyangnyangbot.application.port.in.chat.HandleChatEventUseCase.ChatReceived;

public interface RecordPresenceChatUseCase {

    /** 출석 수집이 활성화되어 있으면 채팅 발신자를 출석 사용자로 기록한다. */
    void recordChatUser(@Valid @NotNull(message = "chat is required") ChatReceived chat);
}
