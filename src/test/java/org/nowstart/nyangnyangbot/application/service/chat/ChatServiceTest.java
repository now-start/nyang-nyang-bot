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
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort.MessageCommand;
import org.nowstart.nyangnyangbot.application.port.out.command.CommandPort;
import org.nowstart.nyangnyangbot.application.port.out.command.CommandPort.CommandRecord;
import org.nowstart.nyangnyangbot.application.port.out.favorite.FavoriteBalanceQueryPort;
import org.nowstart.nyangnyangbot.application.service.attendance.AttendanceService;
import org.nowstart.nyangnyangbot.application.service.command.CommandTemplateRenderer;
import org.nowstart.nyangnyangbot.application.service.command.CommandVariableRegistry;
import org.nowstart.nyangnyangbot.application.service.command.CoreCommandVariableContributor;
import org.nowstart.nyangnyangbot.application.service.command.FavoriteCommandVariableContributor;
import org.nowstart.nyangnyangbot.application.service.weeklychat.WeeklyChatRankService;

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
    private FavoriteBalanceQueryPort favoriteBalanceQueryPort;

    private ChatService chatService;

    @BeforeEach
    void setUp() {
        chatService = service();
    }

    @Test
    void handle_ShouldRecordAttendanceAndWeeklyChatRank() {
        // 준비
        ChatReceived chatDto = chat("안녕", "치즈냥");

        // 실행
        chatService.handle(chatDto);

        // 검증
        BDDMockito.then(attendanceService).should().recordChatUser(any(ChatReceived.class));
        BDDMockito.then(weeklyChatRankService).should().recordChat(any(ChatReceived.class));
    }

    @Test
    void handle_ShouldSuppressSameUserCommandWithinCooldown() {
        // 준비
        chatService = BDDMockito.spy(service());
        doReturn(1_000L, 2_000L, 32_000L).when(chatService).currentTimeMillis();
        given(commandPort.findActiveByTrigger("!호감도"))
                .willReturn(Optional.of(command(
                        10L,
                        "!호감도",
                        "{viewer.nickname}",
                        30
                )));
        ChatReceived chatDto = chat("!호감도", "치즈냥");

        // 실행
        chatService.handle(chatDto);
        chatService.handle(chatDto);
        chatService.handle(chatDto);

        // 검증
        BDDMockito.then(chzzkClientPort)
                .should(BDDMockito.times(2))
                .sendMessage(new MessageCommand("치즈냥"));
    }

    @Test
    void handle_ShouldRenderNamespacedViewerAndInvocationVariables() {
        // 준비
        given(commandPort.findActiveByTrigger("!인사"))
                .willReturn(Optional.of(command(
                        20L,
                        "!인사",
                        "{viewer.nickname} {invocation.command} {invocation.args} "
                                + "{invocation.arg1} {invocation.arg2}",
                        30
                )));
        ChatReceived chatDto = chat("!인사 첫번째 두번째", "치즈냥");

        // 실행
        chatService.handle(chatDto);

        // 검증
        BDDMockito.then(chzzkClientPort).should().sendMessage(new MessageCommand(
                "치즈냥 !인사 첫번째 두번째 첫번째 두번째"
        ));
    }

    @Test
    void handle_ShouldNotResolveVariablesAgainWhenCommandIsSuppressedByCooldown() {
        // 준비
        chatService = BDDMockito.spy(service());
        doReturn(1_000L, 2_000L).when(chatService).currentTimeMillis();
        given(commandPort.findActiveByTrigger("!호감도"))
                .willReturn(Optional.of(command(
                        25L,
                        "!호감도",
                        "{favorite.balance}",
                        30
                )));
        given(favoriteBalanceQueryPort.findBalanceByUserId("user-1")).willReturn(Optional.of(100));
        ChatReceived chatDto = chat("!호감도", "치즈냥");

        // 실행
        chatService.handle(chatDto);
        chatService.handle(chatDto);

        // 검증
        BDDMockito.then(favoriteBalanceQueryPort).should(BDDMockito.times(1))
                .findBalanceByUserId("user-1");
        BDDMockito.then(chzzkClientPort).should(BDDMockito.times(1))
                .sendMessage(new MessageCommand("100"));
    }

    @Test
    void handle_ShouldSkipBlankRenderedResponse() {
        // 준비
        given(commandPort.findActiveByTrigger("!인자"))
                .willReturn(Optional.of(command(
                        26L,
                        "!인자",
                        "{invocation.args}",
                        30
                )));
        ChatReceived chatDto = chat("!인자", "치즈냥");

        // 실행
        chatService.handle(chatDto);

        // 검증
        BDDMockito.then(chzzkClientPort).shouldHaveNoInteractions();
    }

    @Test
    void handle_ShouldRenderZeroWhenFavoriteDoesNotExistUsingReadOnlyQuery() {
        // 준비
        given(commandPort.findActiveByTrigger("!호감도"))
                .willReturn(Optional.of(command(
                        30L,
                        "!호감도",
                        "{viewer.nickname}님의 호감도는 {favorite.balance} 입니다.💛",
                        30
                )));
        given(favoriteBalanceQueryPort.findBalanceByUserId("user-1")).willReturn(Optional.empty());
        ChatReceived chatDto = chat("!호감도", "치즈냥");

        // 실행
        chatService.handle(chatDto);

        // 검증
        BDDMockito.then(favoriteBalanceQueryPort).should().findBalanceByUserId("user-1");
        BDDMockito.then(chzzkClientPort).should().sendMessage(new MessageCommand(
                "치즈냥님의 호감도는 0 입니다.💛"
        ));
    }

    @Test
    void handle_ShouldNotQueryUnusedFavoriteContributor() {
        // 준비
        given(commandPort.findActiveByTrigger("!인사"))
                .willReturn(Optional.of(command(
                        40L,
                        "!인사",
                        "안녕하세요 {viewer.nickname}님!",
                        30
                )));
        ChatReceived chatDto = chat("!인사", "치즈냥");

        // 실행
        chatService.handle(chatDto);

        // 검증
        BDDMockito.then(favoriteBalanceQueryPort).shouldHaveNoInteractions();
        BDDMockito.then(chzzkClientPort).should().sendMessage(new MessageCommand(
                "안녕하세요 치즈냥님!"
        ));
    }

    @Test
    void handle_ShouldUseSenderIdWhenProfileMissing() {
        // 준비
        given(commandPort.findActiveByTrigger("!인사"))
                .willReturn(Optional.of(command(
                        50L,
                        "!인사",
                        "{viewer.nickname}",
                        30
                )));
        ChatReceived chatDto = new ChatReceived(
                "channel-1",
                "user-1",
                null,
                "!인사",
                null,
                1711111111L
        );

        // 실행
        chatService.handle(chatDto);

        // 검증
        BDDMockito.then(chzzkClientPort).should().sendMessage(new MessageCommand("user-1"));
    }

    private ChatService service() {
        CommandVariableRegistry variableRegistry = new CommandVariableRegistry(List.of(
                new CoreCommandVariableContributor(),
                new FavoriteCommandVariableContributor(favoriteBalanceQueryPort)
        ));
        return new ChatService(
                attendanceService,
                weeklyChatRankService,
                commandPort,
                chzzkClientPort,
                templateRenderer,
                variableRegistry
        );
    }

    private ChatReceived chat(String content, String nickname) {
        return new ChatReceived(
                "channel-1",
                "user-1",
                new ChatReceived.Profile(nickname, null, true),
                content,
                null,
                1711111111L
        );
    }

    private CommandRecord command(
            Long id,
            String trigger,
            String messageTemplate,
            Integer cooldownSeconds
    ) {
        return new CommandRecord(
                id,
                trigger,
                messageTemplate,
                true,
                cooldownSeconds,
                "system",
                "system",
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }
}
