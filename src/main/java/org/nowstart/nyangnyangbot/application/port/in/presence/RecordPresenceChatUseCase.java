package org.nowstart.nyangnyangbot.application.port.in.presence;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.nowstart.nyangnyangbot.application.port.in.chat.HandleChatEventUseCase.ChatReceived;

public interface RecordPresenceChatUseCase {

    void recordChatUser(@Valid @NotNull(message = "chat is required") ChatReceived chat);
}
