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
import xyz.r2turntrue.chzzk4j.chat.ChatEventListener;
import xyz.r2turntrue.chzzk4j.chat.ChatMessage;
import xyz.r2turntrue.chzzk4j.chat.ChzzkChat;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ChatService implements ChatEventListener, Emitter.Listener {

    private ChzzkChat chzzkChat;
    private final ObjectMapper objectMapper;
    private final Map<String, Command> commands;

    @Override
    public void onConnect(ChzzkChat chzzkChat, boolean isReconnecting) {
        this.chzzkChat = chzzkChat;
    }

    @Override
    public void onChat(ChatMessage msg) {
        Command command = commands.get(CommandType.findNameByCommand(msg.getContent().split(" ")[0]));
        if (command != null) {
            log.info("[CHAT] : {}", chzzkChat);
            command.v1(chzzkChat, msg);
        }
    }

    @Override
    @SneakyThrows
    public void call(Object... objects) {
        ChatDto chatDto = objectMapper.readValue((String) objects[0], ChatDto.class);
        Command command = commands.get(CommandType.findNameByCommand(chatDto.getContent().split(" ")[0]));
        if (command != null) {
            log.info("[CHAT] : {}", chatDto);
            command.v2(chatDto);
        }
    }
}