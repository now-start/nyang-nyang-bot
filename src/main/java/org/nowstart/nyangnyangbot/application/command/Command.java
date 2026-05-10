package org.nowstart.nyangnyangbot.application.command;

import org.nowstart.nyangnyangbot.application.dto.chzzk.ChatDto;

public interface Command {

    void run(ChatDto chatDto);
}
