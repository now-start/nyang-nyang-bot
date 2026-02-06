package org.nowstart.nyangnyangbot.service.command;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.data.dto.ChatDto;
import org.nowstart.nyangnyangbot.data.dto.MessageRequestDto;
import org.nowstart.nyangnyangbot.data.entity.FavoriteEntity;
import org.nowstart.nyangnyangbot.repository.ChzzkOpenApi;
import org.nowstart.nyangnyangbot.repository.FavoriteRepository;

@ExtendWith(MockitoExtension.class)
class FavoriteTest {

    @Mock
    private ChzzkOpenApi chzzkOpenApi;

    @Mock
    private FavoriteRepository favoriteRepository;

    @InjectMocks
    private Favorite favorite;

    @Test
    void run_ShouldUseExistingFavorite_WhenFound() {
        ChatDto chatDto = buildChatDto("user1", "tester");
        FavoriteEntity existing = FavoriteEntity.builder()
                .userId("user1")
                .nickName("tester")
                .favorite(10)
                .build();

        given(favoriteRepository.findById("user1")).willReturn(java.util.Optional.of(existing));

        favorite.run(chatDto);

        BDDMockito.then(favoriteRepository).should(never()).save(any());
        ArgumentCaptor<MessageRequestDto> captor = ArgumentCaptor.forClass(MessageRequestDto.class);
        BDDMockito.then(chzzkOpenApi).should().sendMessage(captor.capture());
        then(captor.getValue().getMessage()).contains("tester");
        then(captor.getValue().getMessage()).contains("10");
    }

    @Test
    void run_ShouldCreateFavorite_WhenMissing() {
        ChatDto chatDto = buildChatDto("user2", "newbie");
        FavoriteEntity saved = FavoriteEntity.builder()
                .userId("user2")
                .nickName("newbie")
                .favorite(0)
                .build();

        given(favoriteRepository.findById("user2")).willReturn(java.util.Optional.empty());
        given(favoriteRepository.save(any(FavoriteEntity.class))).willReturn(saved);

        favorite.run(chatDto);

        BDDMockito.then(favoriteRepository).should().save(any(FavoriteEntity.class));
        BDDMockito.then(chzzkOpenApi).should().sendMessage(any(MessageRequestDto.class));
    }

    private ChatDto buildChatDto(String senderChannelId, String nickname) {
        return ChatDto.builder()
                .senderChannelId(senderChannelId)
                .profile(ChatDto.Profile.builder()
                        .nickname(nickname)
                        .build())
                .build();
    }
}






