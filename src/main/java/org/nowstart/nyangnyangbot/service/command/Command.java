package org.nowstart.nyangnyangbot.service.command;

import org.nowstart.nyangnyangbot.data.dto.chzzk.ChatDto;

public interface Command {

    void run(ChatDto chatDto);
}
