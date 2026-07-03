package org.nowstart.nyangnyangbot.application.service.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.socket.emitter.Emitter;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.application.port.in.attendance.RecordAttendanceChatUseCase;
import org.nowstart.nyangnyangbot.application.port.in.weeklychat.RecordWeeklyChatUseCase;
import org.nowstart.nyangnyangbot.application.port.out.command.CommandPort;
import org.nowstart.nyangnyangbot.application.port.out.command.CommandPort.CommandRecord;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort.ChatEventPayload;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort.MessageCommand;
import org.nowstart.nyangnyangbot.application.port.out.favorite.FavoriteQueryPort;
import org.nowstart.nyangnyangbot.application.port.out.favorite.FavoriteQueryPort.SummaryResult;
import org.nowstart.nyangnyangbot.application.service.command.CommandHandler;
import org.nowstart.nyangnyangbot.application.service.command.CommandService;
import org.nowstart.nyangnyangbot.application.service.command.CommandTemplateRenderer;
import org.nowstart.nyangnyangbot.application.service.command.CommandTemplateRenderer.TemplateContext;
import org.nowstart.nyangnyangbot.domain.chat.CommandCooldown;
import org.nowstart.nyangnyangbot.domain.type.CommandType;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ChatService implements Emitter.Listener {

    private static final long DEFAULT_COMMAND_COOLDOWN_MILLIS = 30_000L;

    private final ObjectMapper objectMapper;
    private final Map<String, CommandHandler> commandHandlers;
    private final RecordAttendanceChatUseCase recordAttendanceChatUseCase;
    private final RecordWeeklyChatUseCase recordWeeklyChatUseCase;
    private final CommandPort commandPort;
    private final ChzzkClientPort chzzkClientPort;
    private final FavoriteQueryPort favoriteQueryPort;
    private final CommandTemplateRenderer templateRenderer;
    private final CommandCooldown commandCooldown = new CommandCooldown(DEFAULT_COMMAND_COOLDOWN_MILLIS);

    @Override
    @SneakyThrows
    public void call(Object... objects) {
        ChatEventPayload chat = objectMapper.readValue((String) objects[0], ChatEventPayload.class);
        log.info("[ChzzkChat] socket received: {}", chat);
        recordAttendanceChatUseCase.recordChatUser(chat);
        recordWeeklyChatUseCase.recordChat(chat);
        String commandToken = firstToken(chat.content());
        if (commandToken == null || !commandToken.startsWith("!")) {
            return;
        }
        commandPort.findActiveByTrigger(CommandService.normalizeTrigger(commandToken))
                .ifPresent(command -> runCommand(command, chat, commandToken));
    }

    private void runCommand(CommandRecord command, ChatEventPayload chat, String commandToken) {
        if (command.requiredRole() != null && !"USER".equals(command.requiredRole())) {
            return;
        }
        if (isInCooldown(chat.senderChannelId(), String.valueOf(command.id()), command.userCooldownSeconds())) {
            return;
        }
        if (command.type() == CommandType.TEXT) {
            sendTextCommand(command, chat, commandToken);
            return;
        }
        if (command.type() != CommandType.TRIGGER || command.actionKey() == null) {
            return;
        }
        command.actionKey().commandBeanName().ifPresent(commandName -> {
            CommandHandler commandHandler = commandHandlers.get(commandName);
            if (commandHandler != null) {
                commandHandler.run(chat);
            }
        });
    }

    private void sendTextCommand(CommandRecord command, ChatEventPayload chat, String commandToken) {
        String[] tokens = tokens(chat.content());
        SummaryResult favorite = templateRenderer.usesVariable(command.messageTemplate(), "favorite")
                ? favoriteQueryPort.getOrCreate(chat.senderChannelId(), chat.profile().nickname())
                : null;
        String message = templateRenderer.render(
                command.messageTemplate(),
                new TemplateContext(
                        chat.profile().nickname(),
                        commandToken,
                        args(tokens),
                        tokens.length > 1 ? tokens[1] : "",
                        tokens.length > 2 ? tokens[2] : "",
                        favorite == null ? 0 : favorite.favorite(),
                        LocalDateTime.now()
                )
        );
        chzzkClientPort.sendMessage(new MessageCommand(message));
    }

    boolean isInCooldown(String userId, String commandId, Integer cooldownSeconds) {
        long cooldownMillis = (cooldownSeconds == null ? 30L : cooldownSeconds) * 1_000L;
        return commandCooldown.isInCooldown(userId, commandId, currentTimeMillis(), cooldownMillis);
    }

    String firstToken(String content) {
        String[] tokens = tokens(content);
        return tokens.length == 0 ? null : tokens[0];
    }

    private String args(String[] tokens) {
        if (tokens.length <= 1) {
            return "";
        }
        return String.join(" ", Arrays.copyOfRange(tokens, 1, tokens.length));
    }

    private String[] tokens(String content) {
        if (content == null || content.isBlank()) {
            return new String[0];
        }
        return content.trim().split("\\s+");
    }

    long currentTimeMillis() {
        return System.currentTimeMillis();
    }
}
