package org.nowstart.nyangnyangbot.application.service.chat;

import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.application.port.in.attendance.RecordAttendanceChatUseCase;
import org.nowstart.nyangnyangbot.application.port.in.chzzk.HandleChzzkEventUseCase.ChatReceived;
import org.nowstart.nyangnyangbot.application.port.in.timer.RecordTimerChatUseCase;
import org.nowstart.nyangnyangbot.application.port.in.weeklychat.RecordWeeklyChatUseCase;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort.MessageCommand;
import org.nowstart.nyangnyangbot.application.port.out.command.CommandPort;
import org.nowstart.nyangnyangbot.application.port.out.command.CommandPort.CommandRecord;
import org.nowstart.nyangnyangbot.application.service.command.CommandTemplateRenderer;
import org.nowstart.nyangnyangbot.application.service.command.CommandVariableContext;
import org.nowstart.nyangnyangbot.application.service.command.CommandVariableRegistry;
import org.nowstart.nyangnyangbot.domain.chat.CommandCooldown;
import org.nowstart.nyangnyangbot.domain.chat.CommandTrigger;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ChatService {

    private static final long DEFAULT_COMMAND_COOLDOWN_MILLIS = 30_000L;

    private final RecordAttendanceChatUseCase recordAttendanceChatUseCase;
    private final RecordWeeklyChatUseCase recordWeeklyChatUseCase;
    private final RecordTimerChatUseCase recordTimerChatUseCase;
    private final CommandPort commandPort;
    private final ChzzkClientPort chzzkClientPort;
    private final CommandTemplateRenderer templateRenderer;
    private final CommandVariableRegistry variableRegistry;
    private final CommandCooldown commandCooldown = new CommandCooldown(DEFAULT_COMMAND_COOLDOWN_MILLIS);

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
        if (ChatEventSupport.senderChannelId(chat).equals(chat.channelId())) {
            return;
        }
        recordTimerChatUseCase.recordChatActivity();
        String commandToken = firstToken(chat.content());
        if (commandToken == null) {
            return;
        }
        CommandRecord command = commandPort.findActiveCommandsByTrigger()
                .get(CommandTrigger.normalize(commandToken));
        if (command != null) {
            runCommand(command, chat, commandToken);
        }
    }

    private void runCommand(CommandRecord command, ChatReceived chat, String commandToken) {
        String[] tokens = tokens(chat.content());
        String userId = ChatEventSupport.senderChannelId(chat);
        if (isInCooldown(userId, String.valueOf(command.id()), command.userCooldownSeconds())) {
            return;
        }
        CommandVariableContext context = new CommandVariableContext(
                userId,
                ChatEventSupport.displayName(chat),
                commandToken,
                args(tokens),
                tokens.length > 1 ? tokens[1] : "",
                tokens.length > 2 ? tokens[2] : "",
                LocalDateTime.now()
        );
        Set<String> requestedVariables = templateRenderer.variables(command.messageTemplate());
        String message = templateRenderer.render(
                command.messageTemplate(),
                variableRegistry.resolve(requestedVariables, context)
        );
        if (message.isBlank()) {
            log.debug("Command response was blank and will not be sent: commandId={} trigger={}",
                    command.id(), command.trigger());
            return;
        }
        chzzkClientPort.sendMessage(new MessageCommand(message));
        String nickname = ChatEventSupport.displayName(chat);
        log.atInfo()
                .addKeyValue("event", "command.executed")
                .addKeyValue("command_id", command.id())
                .addKeyValue("command_trigger", command.trigger())
                .addKeyValue("user_nickname", nickname)
                .log("event=command.executed commandId={} trigger={} nickname={}",
                        command.id(), command.trigger(), nickname);
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
