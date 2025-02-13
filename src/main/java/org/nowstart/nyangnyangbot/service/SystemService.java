package org.nowstart.nyangnyangbot.service;

import io.socket.emitter.Emitter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SystemService implements Emitter.Listener {

    private final SessionService sessionService;

    @Override
    @SneakyThrows
    public void call(Object... objects) {
        JSONObject message = new JSONObject((String) objects[0]);

        if ("connected".equals(message.getString("type"))) {
            sessionService.connectChat(message.getJSONObject("data").getString("sessionKey"));
        } else {
            log.info("{}", objects);
        }
    }
}