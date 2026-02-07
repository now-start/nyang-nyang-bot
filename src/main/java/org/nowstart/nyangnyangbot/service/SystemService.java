package org.nowstart.nyangnyangbot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.socket.emitter.Emitter;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.data.dto.chzzk.SystemDto;
import org.nowstart.nyangnyangbot.data.property.ChzzkProperty;
import org.nowstart.nyangnyangbot.repository.ChzzkOpenApi;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class SystemService implements Emitter.Listener {

    private String sessionKey;
    private final ObjectMapper objectMapper;
    private final ChzzkProperty chzzkProperty;
    private final ChzzkOpenApi chzzkOpenApi;

    @Override
    @SneakyThrows
    public void call(Object... objects) {
        SystemDto systemDto = objectMapper.readValue((String) objects[0], SystemDto.class);
        log.info("[SYSTEM] : {}", systemDto);

        if ("connected".equalsIgnoreCase(systemDto.type())) {
            sessionKey = systemDto.data().sessionKey();
            chzzkOpenApi.subscribeChatEvent(sessionKey);
        }
    }

    public boolean isConnected() {
        if (sessionKey == null) {
            return false;
        }

        return chzzkOpenApi
                .getSessionList(chzzkProperty.clientId(), chzzkProperty.clientSecret())
                .content().data().stream()
                .filter(sessionData -> sessionData.sessionKey().equals(sessionKey))
                .anyMatch(sessionData -> sessionData.disconnectedDate() == null);
    }

    public String getSession() {
        return chzzkOpenApi.getSession(chzzkProperty.clientId(), chzzkProperty.clientSecret()).content().url();
    }

}
