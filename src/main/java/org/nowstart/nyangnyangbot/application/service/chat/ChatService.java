package org.nowstart.nyangnyangbot.application.service.chat;

import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.application.port.in.chzzk.HandleChzzkEventUseCase.ChatReceived;
import org.nowstart.nyangnyangbot.application.port.in.command.ExecuteCommandUseCase;
import org.nowstart.nyangnyangbot.application.port.in.command.ExecuteCommandUseCase.ApprovedCommand;
import org.nowstart.nyangnyangbot.application.port.in.command.ExecuteCommandUseCase.ExecuteCommand;
import org.nowstart.nyangnyangbot.application.port.in.timer.RecordTimerChatUseCase;
import org.nowstart.nyangnyangbot.application.port.in.presence.RecordPresenceChatUseCase;
import org.nowstart.nyangnyangbot.application.port.in.weeklychat.RecordWeeklyChatUseCase;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort.MessageCommand;
import org.nowstart.nyangnyangbot.application.port.out.command.CommandPort;
import org.nowstart.nyangnyangbot.domain.chat.CommandTrigger;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final RecordPresenceChatUseCase recordPresenceChatUseCase;
    private final RecordWeeklyChatUseCase recordWeeklyChatUseCase;
    private final RecordTimerChatUseCase recordTimerChatUseCase;
    private final ExecuteCommandUseCase executeCommandUseCase;
    private final CommandPort commandPort;
    private final ChzzkClientPort chzzkClientPort;

    public void handle(ChatReceived chat) {
        if (chat == null) {
            return;
        }
        log.info("[ChzzkChat] socket received: {}", chat);
        runBestEffort("presence", () -> recordPresenceChatUseCase.recordChatUser(chat));
        runBestEffort("weekly_chat", () -> recordWeeklyChatUseCase.recordChat(chat));
        if (!ChatEventSupport.hasSenderChannelId(chat)) {
            return;
        }
        if (ChatEventSupport.senderChannelId(chat).equals(chat.channelId())) {
            return;
        }
        runBestEffort("timer_chat", recordTimerChatUseCase::recordChatActivity);
        String commandToken = firstToken(chat.content());
        if (commandToken == null) {
            return;
        }
        if (!commandPort.findActiveCommandsByTrigger().containsKey(CommandTrigger.normalize(commandToken))) {
            return;
        }
        runCommand(chat, commandToken);
    }

    private void runCommand(ChatReceived chat, String commandToken) {
        String[] tokens = tokens(chat.content());
        String userId = ChatEventSupport.senderChannelId(chat);
        var approved = executeCommandUseCase.execute(new ExecuteCommand(
                commandToken,
                userId,
                ChatEventSupport.displayName(chat),
                args(tokens),
                tokens.length > 1 ? tokens[1] : "",
                tokens.length > 2 ? tokens[2] : ""
        ));
        if (approved.isEmpty()) {
            return;
        }
        ApprovedCommand command = approved.get();
        chzzkClientPort.sendMessage(new MessageCommand(command.renderedMessage()));
        String nickname = ChatEventSupport.displayName(chat);
        log.atInfo()
                .addKeyValue("event", "command.executed")
                .addKeyValue("command_id", command.commandId())
                .addKeyValue("command_trigger", command.trigger())
                .addKeyValue("user_nickname", nickname)
                .log("event=command.executed commandId={} trigger={} nickname={}",
                        command.commandId(), command.trigger(), nickname);
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

    private void runBestEffort(String consumer, Runnable operation) {
        try {
            operation.run();
        } catch (RuntimeException failure) {
            log.warn("event=chat.consumer.failed consumer={}", consumer, failure);
        }
    }

}
