package org.nowstart.nyangnyangbot.application.service.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.socket.emitter.Emitter;
import jakarta.transaction.Transactional;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.application.port.in.attendance.RecordAttendanceChatUseCase;
import org.nowstart.nyangnyangbot.application.port.in.weeklychat.RecordWeeklyChatUseCase;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort.ChatEventPayload;
import org.nowstart.nyangnyangbot.domain.chat.ChatCommandCooldown;
import org.nowstart.nyangnyangbot.domain.type.CommandType;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ChatService implements Emitter.Listener {

    private static final long DEFAULT_COMMAND_COOLDOWN_MILLIS = 30_000L;

    private final ObjectMapper objectMapper;
    private final Map<String, Command> commands;
    private final RecordAttendanceChatUseCase recordAttendanceChatUseCase;
    private final RecordWeeklyChatUseCase recordWeeklyChatUseCase;
    private final ChatCommandCooldown commandCooldown = new ChatCommandCooldown(DEFAULT_COMMAND_COOLDOWN_MILLIS);

    @Override
    @SneakyThrows
    public void call(Object... objects) {
        ChatEventPayload chat = objectMapper.readValue((String) objects[0], ChatEventPayload.class);
        log.info("[ChzzkChat] socket received: {}", chat);
        recordAttendanceChatUseCase.recordChatUser(chat);
        recordWeeklyChatUseCase.recordChat(chat);
        String commandName = CommandType.findNameByCommand(chat.content().split(" ")[0]);
        if (commandName == null) {
            return;
        }
        Command command = commands.get(commandName);
        if (command != null && !isInCooldown(chat.senderChannelId(), commandName)) {
            command.run(chat);
        }
    }

    boolean isInCooldown(String userId, String commandName) {
        return commandCooldown.isInCooldown(userId, commandName, currentTimeMillis());
    }

    long currentTimeMillis() {
        return System.currentTimeMillis();
    }
}
