package org.nowstart.nyangnyangbot.adapter.in.scheduler.chzzk;

import java.net.URISyntaxException;
import org.nowstart.nyangnyangbot.application.port.in.chzzk.ConnectChzzkChatSocketUseCase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ChzzkChatConnectionScheduler {

    private final ConnectChzzkChatSocketUseCase connectChzzkChatSocketUseCase;
    private final boolean autoConnectEnabled;

    public ChzzkChatConnectionScheduler(
            ConnectChzzkChatSocketUseCase connectChzzkChatSocketUseCase,
            @Value("${chzzk.chat.auto-connect-enabled:true}") boolean autoConnectEnabled
    ) {
        this.connectChzzkChatSocketUseCase = connectChzzkChatSocketUseCase;
        this.autoConnectEnabled = autoConnectEnabled;
    }

    @Scheduled(fixedDelay = 1000 * 60)
    public void scheduledConnect() throws URISyntaxException {
        if (!autoConnectEnabled) {
            return;
        }
        connectChzzkChatSocketUseCase.connect();
    }
}
