package org.nowstart.chzzk_like_bot.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.chzzk_like_bot.service.OnChatService;
import org.springframework.stereotype.Component;
import xyz.r2turntrue.chzzk4j.chat.ChatEventListener;
import xyz.r2turntrue.chzzk4j.chat.ChatMessage;
import xyz.r2turntrue.chzzk4j.chat.ChzzkChat;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatListener implements ChatEventListener {

    private final OnChatService onChatService;
    private ChzzkChat chat;

    @Override
    public void onConnect(ChzzkChat chat, boolean isReconnecting) {
        this.chat = chat;
    }

    @Override
    public void onChat(ChatMessage msg) {
        onChatService.onChat(chat ,msg);
    }
}