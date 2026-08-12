package org.nowstart.nyangnyangbot.application.service.chzzk;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.application.port.in.chzzk.HandleChzzkSystemEventUseCase;
import org.nowstart.nyangnyangbot.application.port.in.chzzk.HandleChzzkSystemEventUseCase.SystemReceived;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkConfigurationPort;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Slf4j
@Service
@Validated
@RequiredArgsConstructor
public class SystemService implements HandleChzzkSystemEventUseCase {

    private final ChzzkConfigurationPort chzzkConfigurationPort;
    private final ChzzkClientPort chzzkClientPort;
    private boolean connecting;
    private long connectionAttemptSequence;
    private long activeConnectionAttemptId;
    private String sessionKey;

    @Override
    public synchronized void handle(long connectionAttemptId, SystemReceived system) {
        if (connectionAttemptId != activeConnectionAttemptId) {
            return;
        }
        if ("connected".equalsIgnoreCase(system.type())) {
            String connectedSessionKey = system.data().sessionKey();
            if (connectedSessionKey == null || connectedSessionKey.isBlank()) {
                log.warn("[SYSTEM] ignored connected event without session key");
                connecting = false;
                return;
            }
            try {
                chzzkClientPort.subscribeChatEvent(connectedSessionKey);
                chzzkClientPort.subscribeDonationEvent(connectedSessionKey);
                sessionKey = connectedSessionKey;
            } finally {
                connecting = false;
            }
        }
    }

    @Override
    public synchronized void handleConnectionClosed(long connectionAttemptId) {
        if (connectionAttemptId != activeConnectionAttemptId) {
            return;
        }
        activeConnectionAttemptId = 0;
        sessionKey = null;
        connecting = false;
    }

    public synchronized long beginConnection() {
        if (connecting) {
            return 0;
        }
        if (isConnected()) {
            return 0;
        }
        connecting = true;
        activeConnectionAttemptId = ++connectionAttemptSequence;
        return activeConnectionAttemptId;
    }

    public synchronized boolean isConnected() {
        if (sessionKey == null) {
            return false;
        }

        return chzzkClientPort
                .getSessionList(chzzkConfigurationPort.clientId(), chzzkConfigurationPort.clientSecret())
                .data().stream()
                .filter(sessionData -> sessionKey.equals(sessionData.sessionKey()))
                .anyMatch(sessionData -> sessionData.disconnectedDate() == null);
    }

    public String getSession() {
        String url = chzzkClientPort.getSession(
                chzzkConfigurationPort.clientId(),
                chzzkConfigurationPort.clientSecret()
        ).url();
        if (url == null || url.isBlank()) {
            throw new IllegalStateException("CHZZK session URL is missing");
        }
        return url;
    }

}
