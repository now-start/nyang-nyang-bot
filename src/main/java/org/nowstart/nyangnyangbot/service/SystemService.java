package org.nowstart.nyangnyangbot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.socket.emitter.Emitter;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.data.dto.SystemDto;
import org.nowstart.nyangnyangbot.repository.ChzzkOpenApi;
import org.nowstart.nyangnyangbot.repository.ChzzkUnofficialApi;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class SystemService implements Emitter.Listener {

    private final ObjectMapper objectMapper;
    private final ChzzkOpenApi chzzkOpenApi;
    private final ChzzkUnofficialApi chzzkUnofficialApi;

    @Override
    @SneakyThrows
    public void call(Object... objects) {
        SystemDto systemDto = objectMapper.readValue((String) objects[0], SystemDto.class);

        if ("connected".equalsIgnoreCase(systemDto.getType())) {
            chzzkOpenApi.subscribeChatEvent(systemDto.getData().getSessionKey());
        } else {
            log.info("[SYSTEM] : {}", systemDto);
        }
    }

    public String getSession() {
        return chzzkOpenApi.getSession().getContent().getUrl();
    }

    public Boolean isOnline(String channelId) {
        return "open".equalsIgnoreCase(chzzkUnofficialApi.isOnline(channelId).getContent().getStatus());
    }
}