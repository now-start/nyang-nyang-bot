package org.nowstart.nyangnyangbot.application.service.chat;

import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort.ChatEventPayload;

public interface Command {

    void run(ChatEventPayload chat);
}
