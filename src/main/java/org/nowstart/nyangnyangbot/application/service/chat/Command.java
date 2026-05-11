package org.nowstart.nyangnyangbot.application.service.chat;

import org.nowstart.nyangnyangbot.application.port.out.chzzk.dto.ChatDto;

public interface Command {

    void run(ChatDto chatDto);
}
