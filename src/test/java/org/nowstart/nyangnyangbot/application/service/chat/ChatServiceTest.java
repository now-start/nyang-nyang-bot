package org.nowstart.nyangnyangbot.application.service.chat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.doReturn;
import static org.mockito.BDDMockito.given;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.application.port.in.chzzk.HandleChzzkEventUseCase.ChatReceived;
import org.nowstart.nyangnyangbot.application.port.out.command.CommandPort;
import org.nowstart.nyangnyangbot.application.port.out.command.CommandPort.CommandRecord;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort.MessageCommand;
import org.nowstart.nyangnyangbot.application.port.out.favorite.FavoriteQueryPort;
import org.nowstart.nyangnyangbot.application.port.out.favorite.FavoriteQueryPort.SummaryResult;
import org.nowstart.nyangnyangbot.application.service.attendance.AttendanceService;
import org.nowstart.nyangnyangbot.application.service.command.CommandHandler;
import org.nowstart.nyangnyangbot.application.service.command.CommandTemplateRenderer;
import org.nowstart.nyangnyangbot.application.service.weeklychat.WeeklyChatRankService;
import org.nowstart.nyangnyangbot.domain.type.CommandActionKey;
import org.nowstart.nyangnyangbot.domain.type.CommandType;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    private final CommandTemplateRenderer templateRenderer = new CommandTemplateRenderer();

    @Mock
    private AttendanceService attendanceService;

    @Mock
    private WeeklyChatRankService weeklyChatRankService;

    @Mock
    private CommandPort commandPort;

    @Mock
    private ChzzkClientPort chzzkClientPort;

    @Mock
    private FavoriteQueryPort favoriteQueryPort;

    @Mock
    private CommandHandler favoriteCommand;

    private ChatService chatService;

    @BeforeEach
    void setUp() {
        chatService = new ChatService(
                List.of(),
                attendanceService,
                weeklyChatRankService,
                commandPort,
                chzzkClientPort,
                favoriteQueryPort,
                templateRenderer
        );
    }

    @Test
    void handle_ShouldRecordAttendanceAndWeeklyChatRank() {
        // 준비
        ChatReceived chatDto = new ChatReceived(
                "channel-1",
                "user-1",
                new ChatReceived.Profile("치즈냥", null, true),
                "안녕",
                null,
                1711111111L
        );

        // 실행
        chatService.handle(chatDto);

        // 검증
        BDDMockito.then(attendanceService).should().recordChatUser(any(ChatReceived.class));
        BDDMockito.then(weeklyChatRankService).should().recordChat(any(ChatReceived.class));
    }

    @Test
    void handle_ShouldSuppressSameUserCommandWithinCooldown() {
        // 준비
        given(favoriteCommand.actionKey()).willReturn(CommandActionKey.FAVORITE_STATUS);
        chatService = BDDMockito.spy(new ChatService(
                List.of(favoriteCommand),
                attendanceService,
                weeklyChatRankService,
                commandPort,
                chzzkClientPort,
                favoriteQueryPort,
                templateRenderer
        ));
        doReturn(1_000L, 2_000L, 32_000L).when(chatService).currentTimeMillis();
        given(commandPort.findActiveByTrigger("!호감도"))
                .willReturn(Optional.of(command(
                        10L,
                        CommandType.TRIGGER,
                        "!호감도",
                        CommandActionKey.FAVORITE_STATUS,
                        null,
                        30
                )));
        ChatReceived chatDto = new ChatReceived(
                "channel-1",
                "user-1",
                new ChatReceived.Profile("치즈냥", null, true),
                "!호감도",
                null,
                1711111111L
        );

        // 실행
        chatService.handle(chatDto);
        chatService.handle(chatDto);
        chatService.handle(chatDto);

        // 검증
        BDDMockito.then(favoriteCommand).should(BDDMockito.times(2)).run(any(ChatReceived.class));
    }

    @Test
    void handle_ShouldRenderTextCommandWithFavoriteVariable() {
        // 준비
        given(commandPort.findActiveByTrigger("!점수"))
                .willReturn(Optional.of(command(
                        20L,
                        CommandType.TEXT,
                        "!점수",
                        null,
                        "{nickname} {arg1} {favorite}",
                        0
                )));
        given(favoriteQueryPort.getOrCreate("user-1", "치즈냥"))
                .willReturn(new SummaryResult("user-1", "치즈냥", 123));
        ChatReceived chatDto = new ChatReceived(
                "channel-1",
                "user-1",
                new ChatReceived.Profile("치즈냥", null, true),
                "!점수 hello",
                null,
                1711111111L
        );

        // 실행
        chatService.handle(chatDto);

        // 검증
        BDDMockito.then(chzzkClientPort).should().sendMessage(new MessageCommand("치즈냥 hello 123"));
    }

    @Test
    void handle_ShouldRenderTextCommandWithSenderIdWhenProfileMissing() {
        // 준비
        given(commandPort.findActiveByTrigger("!점수"))
                .willReturn(Optional.of(command(
                        20L,
                        CommandType.TEXT,
                        "!점수",
                        null,
                        "{nickname} {favorite}",
                        0
                )));
        given(favoriteQueryPort.getOrCreate("user-1", "user-1"))
                .willReturn(new SummaryResult("user-1", "user-1", 123));
        ChatReceived chatDto = new ChatReceived(
                "channel-1",
                "user-1",
                null,
                "!점수",
                null,
                1711111111L
        );

        // 실행
        chatService.handle(chatDto);

        // 검증
        BDDMockito.then(chzzkClientPort).should().sendMessage(new MessageCommand("user-1 123"));
    }

    private CommandRecord command(
            Long id,
            CommandType type,
            String trigger,
            CommandActionKey actionKey,
            String messageTemplate,
            Integer cooldownSeconds
    ) {
        return new CommandRecord(
                id,
                type,
                trigger,
                actionKey,
                messageTemplate,
                null,
                null,
                true,
                "USER",
                cooldownSeconds,
                "system",
                "system",
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }
}
