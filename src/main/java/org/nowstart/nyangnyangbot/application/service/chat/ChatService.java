package org.nowstart.nyangnyangbot.application.service.chat;

import org.nowstart.nyangnyangbot.application.service.weeklychat.WeeklyChatRankService;
import org.nowstart.nyangnyangbot.application.service.attendance.AttendanceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.socket.emitter.Emitter;
import jakarta.transaction.Transactional;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.dto.ChatDto;
import org.nowstart.nyangnyangbot.domain.type.CommandType;
import org.nowstart.nyangnyangbot.application.service.chat.Command;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ChatService implements Emitter.Listener {

    private static final long DEFAULT_COMMAND_COOLDOWN_MILLIS = 30_000L;

    private final ObjectMapper objectMapper;
    private final Map<String, Command> commands;
    private final AttendanceService attendanceService;
    private final WeeklyChatRankService weeklyChatRankService;
    private final ConcurrentMap<String, Long> lastCommandTimes = new ConcurrentHashMap<>();

    @Override
    @SneakyThrows
    public void call(Object... objects) {
        ChatDto chatDto = objectMapper.readValue((String) objects[0], ChatDto.class);
        log.info("[ChzzkChat] socket received: {}", chatDto);
        attendanceService.recordChatUser(chatDto);
        weeklyChatRankService.recordChat(chatDto);
        String commandName = CommandType.findNameByCommand(chatDto.content().split(" ")[0]);
        if (commandName == null) {
            return;
        }
        Command command = commands.get(commandName);
        if (command != null && !isInCooldown(chatDto.senderChannelId(), commandName)) {
            command.run(chatDto);
        }
    }

    boolean isInCooldown(String userId, String commandName) {
        if (userId == null || userId.isBlank()) {
            return false;
        }
        String key = userId + ":" + commandName;
        long now = currentTimeMillis();
        Long previous = lastCommandTimes.get(key);
        if (previous != null && now - previous < DEFAULT_COMMAND_COOLDOWN_MILLIS) {
            return true;
        }
        lastCommandTimes.put(key, now);
        return false;
    }

    long currentTimeMillis() {
        return System.currentTimeMillis();
    }
}
