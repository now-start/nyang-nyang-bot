package org.nowstart.nyangnyangbot.application.service.chat;

import org.nowstart.nyangnyangbot.application.service.weeklychat.WeeklyChatRankService;
import org.nowstart.nyangnyangbot.application.service.attendance.AttendanceService;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.doReturn;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort.ChatEventPayload;
import org.nowstart.nyangnyangbot.application.service.chat.Command;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AttendanceService attendanceService;

    @Mock
    private WeeklyChatRankService weeklyChatRankService;

    @Mock
    private Command favoriteCommand;

    private ChatService chatService;

    @BeforeEach
    void setUp() {
        chatService = new ChatService(objectMapper, Map.of(), attendanceService, weeklyChatRankService);
    }

    @Test
    void call_ShouldRecordAttendanceAndWeeklyChatRank() throws Exception {
        // 준비
        ChatEventPayload chatDto = new ChatEventPayload(
                "channel-1",
                "user-1",
                new ChatEventPayload.Profile("치즈냥", null, true),
                "안녕",
                null,
                1711111111L
        );

        // 실행
        chatService.call(objectMapper.writeValueAsString(chatDto));

        // 검증
        BDDMockito.then(attendanceService).should().recordChatUser(any(ChatEventPayload.class));
        BDDMockito.then(weeklyChatRankService).should().recordChat(any(ChatEventPayload.class));
    }

    @Test
    void call_ShouldSuppressSameUserCommandWithinCooldown() throws Exception {
        // 준비
        chatService = BDDMockito.spy(new ChatService(
                objectMapper,
                Map.of("favorite", favoriteCommand),
                attendanceService,
                weeklyChatRankService
        ));
        doReturn(1_000L, 2_000L, 32_000L).when(chatService).currentTimeMillis();
        ChatEventPayload chatDto = new ChatEventPayload(
                "channel-1",
                "user-1",
                new ChatEventPayload.Profile("치즈냥", null, true),
                "!호감도",
                null,
                1711111111L
        );

        // 실행
        chatService.call(objectMapper.writeValueAsString(chatDto));
        chatService.call(objectMapper.writeValueAsString(chatDto));
        chatService.call(objectMapper.writeValueAsString(chatDto));

        // 검증
        BDDMockito.then(favoriteCommand).should(BDDMockito.times(2)).run(any(ChatEventPayload.class));
    }
}
