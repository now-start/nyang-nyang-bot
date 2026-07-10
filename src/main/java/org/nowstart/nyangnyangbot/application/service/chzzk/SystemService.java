package org.nowstart.nyangnyangbot.application.service.chzzk;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.application.port.in.chzzk.HandleChzzkEventUseCase.SystemReceived;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkConfigurationPort;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class SystemService {

    private final ChzzkConfigurationPort chzzkConfigurationPort;
    private final ChzzkClientPort chzzkClientPort;
    private volatile String sessionKey;

    public void handle(SystemReceived system) {
        if (system == null || system.data() == null) {
            return;
        }
        log.info("[SYSTEM] : {}", system);

        if ("connected".equalsIgnoreCase(system.type())) {
            String connectedSessionKey = system.data().sessionKey();
            if (connectedSessionKey == null || connectedSessionKey.isBlank()) {
                log.warn("[SYSTEM] ignored connected event without session key");
                return;
            }
            sessionKey = connectedSessionKey;
            chzzkClientPort.subscribeChatEvent(connectedSessionKey);
            // TODO: enable donation event subscription when handling is ready.
            // chzzkOpenApi.subscribeDonationEvent(sessionKey);
            // TODO: enable subscription event subscription when handling is ready.
            // chzzkOpenApi.subscribeSubscriptionEvent(sessionKey);
        }
    }

    public boolean isConnected() {
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
