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
        }
    }

    public String getSession(ChzzkProperty chzzkProperty) {
        return chzzkOpenApi.getSession(chzzkProperty.getClientId(), chzzkProperty.getClientSecret()).getContent().getUrl();
    }

    public void subscribeChatEvent() {
        chzzkOpenApi.subscribeChatEvent(sessionKey);
    }
}