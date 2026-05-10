package org.nowstart.nyangnyangbot.application.command;

import org.nowstart.nyangnyangbot.application.chzzk.dto.ChatDto;

public interface Command {

    void run(ChatDto chatDto);
}
