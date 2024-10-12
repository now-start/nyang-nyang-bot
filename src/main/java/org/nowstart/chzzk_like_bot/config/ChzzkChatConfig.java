package org.nowstart.chzzk_like_bot.config;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import xyz.r2turntrue.chzzk4j.Chzzk;
import xyz.r2turntrue.chzzk4j.ChzzkBuilder;
import xyz.r2turntrue.chzzk4j.chat.ChzzkChat;

@Configuration
@RequiredArgsConstructor
public class ChzzkChatConfig {

    @Value("${chzzk.channelId}")
    private String channelId;
    @Value("${chzzk.aut}")
    private String aut;
    @Value("${chzzk.ses}")
    private String ses;
    private final ChzzkChatListenerConfig chzzkChatListenerConfig;

    @PostConstruct
    //TODO: aut, ses 값을 지속적으로 들고올 방법을 찾아야 됨
    public void startChat() throws IOException {
        Chzzk chzzk = new ChzzkBuilder()
            .withAuthorization(aut, ses)
            .build();
        ChzzkChat chat = chzzk.chat(channelId)
            .withChatListener(chzzkChatListenerConfig)
            .build();
        chat.connectAsync();
    }
}