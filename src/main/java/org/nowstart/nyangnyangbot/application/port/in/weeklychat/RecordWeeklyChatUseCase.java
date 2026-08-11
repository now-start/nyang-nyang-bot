package org.nowstart.nyangnyangbot.application.port.in.weeklychat;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.nowstart.nyangnyangbot.application.port.in.chat.HandleChatEventUseCase.ChatReceived;

public interface RecordWeeklyChatUseCase {

    void recordChat(@Valid @NotNull(message = "chat is required") ChatReceived chat);
}
