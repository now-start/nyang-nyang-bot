package org.nowstart.nyangnyangbot.service.command;

import org.nowstart.nyangnyangbot.data.dto.ChatDto;

public interface Command {

    void run(ChatDto chatDto);
}