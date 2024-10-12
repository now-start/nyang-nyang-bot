package org.nowstart.chzzk_like_bot.config;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.nowstart.chzzk_like_bot.command.Command;
import org.springframework.stereotype.Component;
import xyz.r2turntrue.chzzk4j.chat.ChatEventListener;
import xyz.r2turntrue.chzzk4j.chat.ChatMessage;
import xyz.r2turntrue.chzzk4j.chat.ChzzkChat;

@Component
@RequiredArgsConstructor
public class ChzzkChatListenerConfig implements ChatEventListener {

    private ChzzkChat chat;
    private final Map<String, Command> commands;

    @Override
    public void onConnect(ChzzkChat chat, boolean isReconnecting) {
        this.chat = chat;
    }

    @Override
    public void onChat(ChatMessage msg) {
        Command command = commands.get(msg.getContent().split(" ")[0]);
        if (command != null) {
            command.execute(chat, msg);
        }
    }
}