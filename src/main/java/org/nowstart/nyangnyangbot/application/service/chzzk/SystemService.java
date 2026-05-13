package org.nowstart.nyangnyangbot.application.service.chzzk;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.socket.emitter.Emitter;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort.SystemEventPayload;
import org.nowstart.nyangnyangbot.config.property.ChzzkProperty;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class SystemService implements Emitter.Listener {

    private String sessionKey;
    private final ObjectMapper objectMapper;
    private final ChzzkProperty chzzkProperty;
    private final ChzzkClientPort chzzkClientPort;

    @Override
    @SneakyThrows
    public void call(Object... objects) {
        SystemEventPayload system = objectMapper.readValue((String) objects[0], SystemEventPayload.class);
        log.info("[SYSTEM] : {}", system);

        if ("connected".equalsIgnoreCase(system.type())) {
            sessionKey = system.data().sessionKey();
            chzzkClientPort.subscribeChatEvent(sessionKey);
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
                .getSessionList(chzzkProperty.clientId(), chzzkProperty.clientSecret())
                .content().data().stream()
                .filter(sessionData -> sessionData.sessionKey().equals(sessionKey))
                .anyMatch(sessionData -> sessionData.disconnectedDate() == null);
    }

    public String getSession() {
        return chzzkClientPort.getSession(chzzkProperty.clientId(), chzzkProperty.clientSecret()).content().url();
    }

}
