package org.nowstart.nyangnyangbot.application.service.command;

import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort.ChatEventPayload;

public interface CommandHandler {

    void run(ChatEventPayload chat);
}
