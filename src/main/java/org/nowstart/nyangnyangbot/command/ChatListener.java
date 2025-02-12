package org.nowstart.nyangnyangbot.command;

import io.socket.emitter.Emitter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import xyz.r2turntrue.chzzk4j.chat.ChzzkChat;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatListener implements Emitter.Listener {

    private final Map<String, Command> commands;

    @Override
    public void call(Object... objects) {
        log.info("{}", objects);
    }
}