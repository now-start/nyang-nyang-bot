package org.nowstart.nyangnyangbot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.socket.emitter.Emitter;
import jakarta.transaction.Transactional;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.data.dto.ChatDto;
import org.nowstart.nyangnyangbot.data.type.CommandType;
import org.nowstart.nyangnyangbot.service.command.Command;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ChatService implements Emitter.Listener {

    private final ObjectMapper objectMapper;
    private final Map<String, Command> commands;

    @Override
    @SneakyThrows
    public void call(Object... objects) {
        ChatDto chatDto = objectMapper.readValue((String) objects[0], ChatDto.class);
        log.info("[ChzzkChat] socket received: {}", chatDto);
        Command command = commands.get(CommandType.findNameByCommand(chatDto.getContent().split(" ")[0]));
        if (command != null) {
            command.run(chatDto);
        }
    }
}