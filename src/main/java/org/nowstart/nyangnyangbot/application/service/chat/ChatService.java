package org.nowstart.nyangnyangbot.application.service.chat;

import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.application.port.in.chzzk.HandleChzzkEventUseCase.ChatReceived;
import org.nowstart.nyangnyangbot.application.port.in.attendance.RecordAttendanceChatUseCase;
import org.nowstart.nyangnyangbot.application.port.in.weeklychat.RecordWeeklyChatUseCase;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort.MessageCommand;
import org.nowstart.nyangnyangbot.application.port.out.command.CommandPort;
import org.nowstart.nyangnyangbot.application.port.out.command.CommandPort.CommandRecord;
import org.nowstart.nyangnyangbot.application.port.out.favorite.FavoriteQueryPort;
import org.nowstart.nyangnyangbot.application.port.out.favorite.FavoriteQueryPort.SummaryResult;
import org.nowstart.nyangnyangbot.application.service.command.CommandHandler;
import org.nowstart.nyangnyangbot.application.service.command.CommandTemplateRenderer;
import org.nowstart.nyangnyangbot.application.service.command.CommandTemplateRenderer.TemplateContext;
import org.nowstart.nyangnyangbot.domain.chat.CommandCooldown;
import org.nowstart.nyangnyangbot.domain.chat.CommandTrigger;
import org.nowstart.nyangnyangbot.domain.type.CommandActionKey;
import org.nowstart.nyangnyangbot.domain.type.CommandType;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Transactional
public class ChatService {

    private static final long DEFAULT_COMMAND_COOLDOWN_MILLIS = 30_000L;

    private final Map<CommandActionKey, CommandHandler> commandHandlers;
    private final RecordAttendanceChatUseCase recordAttendanceChatUseCase;
    private final RecordWeeklyChatUseCase recordWeeklyChatUseCase;
    private final CommandPort commandPort;
    private final ChzzkClientPort chzzkClientPort;
    private final FavoriteQueryPort favoriteQueryPort;
    private final CommandTemplateRenderer templateRenderer;
    private final CommandCooldown commandCooldown = new CommandCooldown(DEFAULT_COMMAND_COOLDOWN_MILLIS);

    public ChatService(
            List<CommandHandler> commandHandlers,
            RecordAttendanceChatUseCase recordAttendanceChatUseCase,
            RecordWeeklyChatUseCase recordWeeklyChatUseCase,
            CommandPort commandPort,
            ChzzkClientPort chzzkClientPort,
            FavoriteQueryPort favoriteQueryPort,
            CommandTemplateRenderer templateRenderer
    ) {
        this.commandHandlers = commandHandlers(commandHandlers);
        this.recordAttendanceChatUseCase = recordAttendanceChatUseCase;
        this.recordWeeklyChatUseCase = recordWeeklyChatUseCase;
        this.commandPort = commandPort;
        this.chzzkClientPort = chzzkClientPort;
        this.favoriteQueryPort = favoriteQueryPort;
        this.templateRenderer = templateRenderer;
    }

    public void handle(ChatReceived chat) {
        if (chat == null) {
            return;
        }
        log.info("[ChzzkChat] socket received: {}", chat);
        recordAttendanceChatUseCase.recordChatUser(chat);
        recordWeeklyChatUseCase.recordChat(chat);
        if (!ChatEventSupport.hasSenderChannelId(chat)) {
            return;
        }
        String commandToken = firstToken(chat.content());
        if (commandToken == null || !commandToken.startsWith("!")) {
            return;
        }
        commandPort.findActiveByTrigger(CommandTrigger.normalize(commandToken))
                .ifPresent(command -> runCommand(command, chat, commandToken));
    }

    private void runCommand(CommandRecord command, ChatReceived chat, String commandToken) {
        if (command.requiredRole() != null && !"USER".equals(command.requiredRole())) {
            return;
        }
        String userId = ChatEventSupport.senderChannelId(chat);
        if (isInCooldown(userId, String.valueOf(command.id()), command.userCooldownSeconds())) {
            return;
        }
        if (command.type() == CommandType.TEXT) {
            sendTextCommand(command, chat, commandToken);
            return;
        }
        if (command.type() != CommandType.TRIGGER || command.actionKey() == null) {
            return;
        }
        CommandHandler commandHandler = commandHandlers.get(command.actionKey());
        if (commandHandler != null) {
            commandHandler.run(chat);
        }
    }

    private Map<CommandActionKey, CommandHandler> commandHandlers(List<CommandHandler> handlers) {
        Map<CommandActionKey, CommandHandler> result = new EnumMap<>(CommandActionKey.class);
        for (CommandHandler handler : handlers) {
            CommandActionKey actionKey = Objects.requireNonNull(handler.actionKey(), "command actionKey is required");
            CommandHandler previous = result.put(actionKey, handler);
            if (previous != null) {
                throw new IllegalStateException("duplicate command handler: " + actionKey);
            }
        }
        return Map.copyOf(result);
    }

    private void sendTextCommand(CommandRecord command, ChatReceived chat, String commandToken) {
        String[] tokens = tokens(chat.content());
        String userId = ChatEventSupport.senderChannelId(chat);
        String displayName = ChatEventSupport.displayName(chat);
        SummaryResult favorite = templateRenderer.usesVariable(command.messageTemplate(), "favorite")
                ? favoriteQueryPort.getOrCreate(userId, displayName)
                : null;
        String message = templateRenderer.render(
                command.messageTemplate(),
                new TemplateContext(
                        displayName,
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
