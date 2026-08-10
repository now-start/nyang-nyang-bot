package org.nowstart.nyangnyangbot.application.service.chzzk;

import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.application.port.in.chzzk.ConnectChzzkChatUseCase;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkChatSocketPort;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChzzkChatConnectionService implements ConnectChzzkChatUseCase {

    private final SystemService systemService;
    private final ChzzkChatSocketPort chzzkChatSocketPort;

    @Override
    public synchronized void connect() {
        long connectionAttemptId = systemService.beginConnection();
        if (connectionAttemptId == 0) {
            return;
        }
        try {
            chzzkChatSocketPort.connect(systemService.getSession(), connectionAttemptId);
        } catch (RuntimeException | Error failure) {
            systemService.handleConnectionClosed(connectionAttemptId);
            throw failure;
        }
    }
}
