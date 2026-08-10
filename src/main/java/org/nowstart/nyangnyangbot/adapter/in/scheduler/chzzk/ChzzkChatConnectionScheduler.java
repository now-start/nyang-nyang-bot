package org.nowstart.nyangnyangbot.adapter.in.scheduler.chzzk;

import org.nowstart.nyangnyangbot.application.port.in.chzzk.ConnectChzzkChatUseCase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ChzzkChatConnectionScheduler {

    private final ConnectChzzkChatUseCase connectChzzkChatUseCase;
    private final boolean autoConnectEnabled;

    public ChzzkChatConnectionScheduler(
            ConnectChzzkChatUseCase connectChzzkChatUseCase,
            @Value("${chzzk.chat.auto-connect-enabled:true}") boolean autoConnectEnabled
    ) {
        this.connectChzzkChatUseCase = connectChzzkChatUseCase;
        this.autoConnectEnabled = autoConnectEnabled;
    }

    @Scheduled(fixedDelayString = "${nyang.chzzk.connection.scheduler-delay-millis:60000}")
    public void scheduledConnect() {
        if (!autoConnectEnabled) {
            return;
        }
        connectChzzkChatUseCase.connect();
    }
}
