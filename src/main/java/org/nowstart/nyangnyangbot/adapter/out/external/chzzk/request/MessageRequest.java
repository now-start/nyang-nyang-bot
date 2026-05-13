package org.nowstart.nyangnyangbot.adapter.out.external.chzzk.request;

import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort.MessageCommand;

public record MessageRequest(String message) {

    public static MessageRequest from(MessageCommand command) {
        return new MessageRequest(command.message());
    }
}
