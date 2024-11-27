package org.nowstart.nyangnyangbot.command;

import xyz.r2turntrue.chzzk4j.chat.ChatMessage;
import xyz.r2turntrue.chzzk4j.chat.ChzzkChat;

public interface Command {
    void execute(ChzzkChat chat, ChatMessage msg);
}