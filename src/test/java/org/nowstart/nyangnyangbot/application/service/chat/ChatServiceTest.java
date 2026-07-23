package org.nowstart.nyangnyangbot.application.service.chat;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;

import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.application.port.in.chzzk.HandleChzzkEventUseCase.ChatReceived;
import org.nowstart.nyangnyangbot.application.port.in.command.ExecuteCommandUseCase;
import org.nowstart.nyangnyangbot.application.port.in.command.ExecuteCommandUseCase.ApprovedCommand;
import org.nowstart.nyangnyangbot.application.port.in.command.ExecuteCommandUseCase.ExecuteCommand;
import org.nowstart.nyangnyangbot.application.port.in.presence.RecordPresenceChatUseCase;
import org.nowstart.nyangnyangbot.application.port.in.timer.RecordTimerChatUseCase;
import org.nowstart.nyangnyangbot.application.port.in.weeklychat.RecordWeeklyChatUseCase;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort.MessageCommand;
import org.nowstart.nyangnyangbot.application.port.out.command.CommandPort;
import org.nowstart.nyangnyangbot.application.port.out.command.CommandPort.CommandRecord;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private RecordPresenceChatUseCase presenceChatUseCase;
    @Mock
    private RecordWeeklyChatUseCase weeklyChatUseCase;
    @Mock
    private RecordTimerChatUseCase timerChatUseCase;
    @Mock
    private ExecuteCommandUseCase executeCommandUseCase;
    @Mock
    private CommandPort commandPort;
    @Mock
    private ChzzkClientPort chzzkClientPort;

    private ChatService service;

    @BeforeEach
    void setUp() {
        service = new ChatService(
                presenceChatUseCase,
                weeklyChatUseCase,
                timerChatUseCase,
                executeCommandUseCase,
                commandPort,
                chzzkClientPort
        );
    }

    @Test
    void handle_PrefiltersOrdinaryChatWithoutStartingCommandTransaction() {
        ChatReceived chat = chat("그냥 채팅");
        given(commandPort.findActiveCommandsByTrigger()).willReturn(Map.of());

        service.handle(chat);

        verify(presenceChatUseCase).recordChatUser(chat);
        verify(weeklyChatUseCase).recordChat(chat);
        verify(timerChatUseCase).recordChatActivity();
        verify(executeCommandUseCase, never()).execute(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void handle_SendsOnlyAfterApprovedCommandReturnsFromTransactionalUseCase() {
        ChatReceived chat = chat("!인사 첫번째 두번째");
        given(commandPort.findActiveCommandsByTrigger()).willReturn(Map.of("!인사", org.mockito.Mockito.mock(
                CommandRecord.class
        )));
        given(executeCommandUseCase.execute(org.mockito.ArgumentMatchers.any())).willReturn(Optional.of(
                new ApprovedCommand(7L, "!인사", "안녕하세요", 10, 3, 0, 0)
        ));

        service.handle(chat);

        ArgumentCaptor<ExecuteCommand> captor = ArgumentCaptor.forClass(ExecuteCommand.class);
        verify(executeCommandUseCase).execute(captor.capture());
        then(captor.getValue().args()).isEqualTo("첫번째 두번째");
        then(captor.getValue().arg1()).isEqualTo("첫번째");
        then(captor.getValue().arg2()).isEqualTo("두번째");
        verify(chzzkClientPort).sendMessage(new MessageCommand("안녕하세요"));
    }

    @Test
    void handle_AuxiliaryAggregationFailureDoesNotDropCommand() {
        ChatReceived chat = chat("!인사");
        doThrow(new IllegalStateException("weekly unavailable")).when(weeklyChatUseCase).recordChat(chat);
        doThrow(new IllegalStateException("timer unavailable")).when(timerChatUseCase).recordChatActivity();
        given(commandPort.findActiveCommandsByTrigger()).willReturn(Map.of(
                "!인사", org.mockito.Mockito.mock(CommandRecord.class)
        ));
        given(executeCommandUseCase.execute(org.mockito.ArgumentMatchers.any())).willReturn(Optional.of(
                new ApprovedCommand(7L, "!인사", "안녕하세요", 1, 1, 0, 0)
        ));

        service.handle(chat);

        verify(executeCommandUseCase).execute(org.mockito.ArgumentMatchers.any());
        verify(chzzkClientPort).sendMessage(new MessageCommand("안녕하세요"));
    }

    private ChatReceived chat(String content) {
        return new ChatReceived(
                "channel-1",
                "user-1",
                new ChatReceived.Profile("치즈냥", null, true),
                content,
                null,
                1711111111L
        );
    }
}
