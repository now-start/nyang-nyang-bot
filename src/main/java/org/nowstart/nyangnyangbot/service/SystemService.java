package org.nowstart.nyangnyangbot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.socket.emitter.Emitter;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.data.dto.SystemDto;
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
    private final ChzzkOpenApi chzzkOpenApi;

    @Override
    @SneakyThrows
    public void call(Object... objects) {
        SystemDto systemDto = objectMapper.readValue((String) objects[0], SystemDto.class);
        log.info("[SYSTEM] : {}", systemDto);

        if ("connected".equalsIgnoreCase(systemDto.getType())) {
            sessionKey = systemDto.getData().getSessionKey();
            chzzkOpenApi.subscribeChatEvent(sessionKey);
        }
    }

    public boolean isConnected(ChzzkProperty chzzkProperty) {
        if (sessionKey == null) {
            return false;
        }

        return chzzkOpenApi
            .getSessionList(chzzkProperty.getClientId(), chzzkProperty.getClientSecret(), "50")
            .getContent().getData().stream()
            .filter(sessionData -> sessionData.getSessionKey().equals(sessionKey))
            .anyMatch(sessionData -> sessionData.getDisconnectedDate() == null);
    }

    public String getSession(ChzzkProperty chzzkProperty) {
        return chzzkOpenApi.getSession(chzzkProperty.getClientId(), chzzkProperty.getClientSecret()).getContent().getUrl();
    }

}