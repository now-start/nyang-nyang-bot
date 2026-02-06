package org.nowstart.nyangnyangbot.service;

import org.nowstart.nyangnyangbot.data.dto.ChatDto;

public interface Command {

    void run(ChatDto chatDto);
}