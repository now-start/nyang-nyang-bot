package org.nowstart.nyangnyangbot.application.service.command;

import org.nowstart.nyangnyangbot.application.port.in.chzzk.HandleChzzkEventUseCase.ChatReceived;
import org.nowstart.nyangnyangbot.domain.type.CommandActionKey;

public interface CommandHandler {

    CommandActionKey actionKey();

    void run(ChatReceived chat);
}
