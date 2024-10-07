package org.nowstart.chzzk_like_bot.config;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import javax.annotation.PostConstruct;
import org.nowstart.chzzk_like_bot.listener.ChatListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import xyz.r2turntrue.chzzk4j.Chzzk;
import xyz.r2turntrue.chzzk4j.ChzzkBuilder;
import xyz.r2turntrue.chzzk4j.chat.ChzzkChat;

@Configuration
public class ChzzkChatConfiguration {

    @Value("${chzzk.channelId}")
    private String channelId;

    @PostConstruct
    public void startChat() throws IOException, InterruptedException {
        Chzzk chzzk = new ChzzkBuilder().build();
        ChzzkChat chat = chzzk.chat(channelId)
            .withChatListener(new ChatListener())
            .withAutoReconnect(true)
            .build();

        chat.connectBlocking();
    }
}