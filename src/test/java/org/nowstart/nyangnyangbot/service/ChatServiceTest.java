package org.nowstart.nyangnyangbot.service;

import static org.mockito.ArgumentMatchers.any;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.data.dto.chzzk.ChatDto;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AttendanceService attendanceService;

    @Mock
    private WeeklyChatRankService weeklyChatRankService;

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
}
