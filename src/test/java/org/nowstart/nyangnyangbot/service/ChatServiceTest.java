package org.nowstart.nyangnyangbot.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.data.dto.ChatDto;
import org.nowstart.nyangnyangbot.service.command.Favorite;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private Map<String, Favorite> commands;

    @Mock
    private Favorite mockCommand;

    @InjectMocks
    private ChatService chatService;

    private ChatDto chatDto;
    private String jsonString;

    @BeforeEach
    void setUp() {
        chatDto = ChatDto.builder()
                .channelId("channel123")
                .senderChannelId("sender123")
                .content("!호감도")
                .profile(ChatDto.Profile.builder()
                        .nickname("testUser")
                        .verifiedMark(false)
                        .build())
                .messageTime(System.currentTimeMillis())
                .build();

        jsonString = "{\"content\":\"!호감도\"}";
    }

    @Test
    void call_ShouldExecuteCommand_WhenCommandExists() throws Exception {
        // given
        given(objectMapper.readValue(jsonString, ChatDto.class)).willReturn(chatDto);
        given(commands.get("favorite")).willReturn(mockCommand);

        // when
        chatService.call(jsonString);

        // then
        BDDMockito.then(objectMapper).should().readValue(jsonString, ChatDto.class);
        BDDMockito.then(commands).should().get("favorite");
        BDDMockito.then(mockCommand).should().run(chatDto);
    }

    @Test
    void call_ShouldNotExecuteCommand_WhenCommandNotFound() throws Exception {
        // given
        ChatDto unknownCommandDto = ChatDto.builder()
                .content("!unknown")
                .build();
        String unknownJson = "{\"content\":\"!unknown\"}";

        given(objectMapper.readValue(unknownJson, ChatDto.class)).willReturn(unknownCommandDto);
        given(commands.get(null)).willReturn(null);

        // when
        chatService.call(unknownJson);

        // then
        BDDMockito.then(objectMapper).should().readValue(unknownJson, ChatDto.class);
        BDDMockito.then(commands).should().get(null);
        BDDMockito.then(mockCommand).should(never()).run(any());
    }

    @Test
    void call_ShouldHandleMessageWithMultipleWords() throws Exception {
        // given
        ChatDto multiWordDto = ChatDto.builder()
                .content("!호감도 추가 정보")
                .build();
        String multiWordJson = "{\"content\":\"!호감도 추가 정보\"}";

        given(objectMapper.readValue(multiWordJson, ChatDto.class)).willReturn(multiWordDto);
        given(commands.get("favorite")).willReturn(mockCommand);

        // when
        chatService.call(multiWordJson);

        // then
        BDDMockito.then(objectMapper).should().readValue(multiWordJson, ChatDto.class);
        BDDMockito.then(commands).should().get("favorite");
        BDDMockito.then(mockCommand).should().run(multiWordDto);
    }

    @Test
    void call_ShouldHandleEmptyContent() throws Exception {
        // given
        ChatDto emptyContentDto = ChatDto.builder()
                .content("")
                .build();
        String emptyJson = "{\"content\":\"\"}";

        given(objectMapper.readValue(emptyJson, ChatDto.class)).willReturn(emptyContentDto);
        given(commands.get(null)).willReturn(null);

        // when
        chatService.call(emptyJson);

        // then
        BDDMockito.then(objectMapper).should().readValue(emptyJson, ChatDto.class);
        BDDMockito.then(mockCommand).should(never()).run(any());
    }
}
