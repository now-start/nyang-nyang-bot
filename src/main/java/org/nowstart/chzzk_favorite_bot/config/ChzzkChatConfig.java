package org.nowstart.chzzk_favorite_bot.config;

import jakarta.annotation.PostConstruct;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import xyz.r2turntrue.chzzk4j.Chzzk;
import xyz.r2turntrue.chzzk4j.ChzzkBuilder;
import xyz.r2turntrue.chzzk4j.chat.ChzzkChat;

@Slf4j
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
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private ChzzkChat activeChat;

    @PostConstruct
    public void startChat() {
        Chzzk chzzk = new ChzzkBuilder()
            .withAuthorization(aut, ses)
            .build();

        executor.scheduleAtFixedRate(() -> {
            try {
                boolean currentBroadcastingStatus = chzzk.getChannel(channelId).isBroadcasting();
                if (currentBroadcastingStatus && activeChat == null) {
                    log.info("====================[START][CHAT]====================");
                    activeChat = chzzk.chat(channelId)
                        .withChatListener(chzzkChatListenerConfig)
                        .withAutoReconnect(false)
                        .build();
                    activeChat.connectBlocking();
                } else if (!currentBroadcastingStatus && activeChat != null) {
                    log.info("====================[END][CHAT]====================");
                    activeChat.closeBlocking();
                    activeChat = null;
                }
            } catch (Exception e) {
                log.error("====================[ERROR][CHAT]====================", e);
            }
        }, 0, 1, TimeUnit.SECONDS);
    }
}