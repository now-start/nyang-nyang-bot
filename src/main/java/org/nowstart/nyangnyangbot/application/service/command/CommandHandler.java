package org.nowstart.nyangnyangbot.application.service.command;

import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort.ChatEventPayload;
import org.nowstart.nyangnyangbot.domain.type.CommandActionKey;

public interface CommandHandler {

    CommandActionKey actionKey();

    void run(ChatEventPayload chat);
}
