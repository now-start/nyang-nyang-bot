package org.nowstart.nyangnyangbot.service.command;

import org.nowstart.nyangnyangbot.data.dto.ChatDto;
import xyz.r2turntrue.chzzk4j.chat.ChatMessage;
import xyz.r2turntrue.chzzk4j.chat.ChzzkChat;

public interface Command {

    void v1(ChzzkChat chat, ChatMessage msg);

    void v2(ChatDto chatDto);
}