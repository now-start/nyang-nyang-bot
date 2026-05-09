package org.nowstart.nyangnyangbot.service;

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
import org.nowstart.nyangnyangbot.data.dto.chzzk.ChatDto;
import org.nowstart.nyangnyangbot.service.command.Command;

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
        ChatDto chatDto = new ChatDto(
                "channel-1",
                "user-1",
                new ChatDto.Profile("치즈냥", null, true),
                "안녕",
                null,
                1711111111L
        );

        chatService.call(objectMapper.writeValueAsString(chatDto));

        BDDMockito.then(attendanceService).should().recordChatUser(any(ChatDto.class));
        BDDMockito.then(weeklyChatRankService).should().recordChat(any(ChatDto.class));
    }

    @Test
    void call_ShouldSuppressSameUserCommandWithinCooldown() throws Exception {
        chatService = BDDMockito.spy(new ChatService(
                objectMapper,
                Map.of("favorite", favoriteCommand),
                attendanceService,
                weeklyChatRankService
        ));
        doReturn(1_000L, 2_000L, 32_000L).when(chatService).currentTimeMillis();
        ChatDto chatDto = new ChatDto(
                "channel-1",
                "user-1",
                new ChatDto.Profile("치즈냥", null, true),
                "!호감도",
                null,
                1711111111L
        );

        chatService.call(objectMapper.writeValueAsString(chatDto));
        chatService.call(objectMapper.writeValueAsString(chatDto));
        chatService.call(objectMapper.writeValueAsString(chatDto));

        BDDMockito.then(favoriteCommand).should(BDDMockito.times(2)).run(any(ChatDto.class));
    }
}
