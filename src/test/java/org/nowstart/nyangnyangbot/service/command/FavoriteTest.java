package org.nowstart.nyangnyangbot.service.command;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;

import java.util.ArrayList;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
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

    private ChatDto chatDto;

    @BeforeEach
    void setUp() {
        chatDto = ChatDto.builder()
                .channelId("channel123")
                .senderChannelId("sender123")
                .content("!호감도")
                .profile(ChatDto.Profile.builder()
                        .nickname("테스트유저")
                        .verifiedMark(false)
                        .build())
                .messageTime(System.currentTimeMillis())
                .build();
    }

    @Test
    void run_ShouldReturnExistingFavorite_WhenUserExists() {
        // given
        FavoriteEntity existingEntity = FavoriteEntity.builder()
                .userId("sender123")
                .nickName("테스트유저")
                .favorite(100)
                .favoriteHistoryEntityList(new ArrayList<>())
                .build();

        given(favoriteRepository.findById("sender123")).willReturn(Optional.of(existingEntity));

        // when
        favorite.run(chatDto);

        // then
        ArgumentCaptor<MessageRequestDto> messageCaptor = ArgumentCaptor.forClass(MessageRequestDto.class);
        BDDMockito.then(chzzkOpenApi).should().sendMessage(messageCaptor.capture());

        MessageRequestDto sentMessage = messageCaptor.getValue();
        then(sentMessage.getMessage()).isEqualTo("테스트유저님의 호감도는 100 입니다.💛");
        BDDMockito.then(favoriteRepository).should().findById("sender123");
        BDDMockito.then(favoriteRepository).should(never()).save(any());
    }

    @Test
    void run_ShouldCreateNewUser_WhenUserNotExists() {
        // given
        FavoriteEntity newEntity = FavoriteEntity.builder()
                .userId("sender123")
                .nickName("테스트유저")
                .favorite(0)
                .favoriteHistoryEntityList(new ArrayList<>())
                .build();

        given(favoriteRepository.findById("sender123")).willReturn(Optional.empty());
        given(favoriteRepository.save(any(FavoriteEntity.class))).willReturn(newEntity);

        // when
        favorite.run(chatDto);

        // then
        ArgumentCaptor<FavoriteEntity> entityCaptor = ArgumentCaptor.forClass(FavoriteEntity.class);
        BDDMockito.then(favoriteRepository).should().save(entityCaptor.capture());

        FavoriteEntity savedEntity = entityCaptor.getValue();
        then(savedEntity.getUserId()).isEqualTo("sender123");
        then(savedEntity.getNickName()).isEqualTo("테스트유저");
        then(savedEntity.getFavorite()).isEqualTo(0);

        ArgumentCaptor<MessageRequestDto> messageCaptor = ArgumentCaptor.forClass(MessageRequestDto.class);
        BDDMockito.then(chzzkOpenApi).should().sendMessage(messageCaptor.capture());

        MessageRequestDto sentMessage = messageCaptor.getValue();
        then(sentMessage.getMessage()).isEqualTo("테스트유저님의 호감도는 0 입니다.💛");
    }

    @Test
    void run_ShouldHandleZeroFavorite() {
        // given
        FavoriteEntity zeroFavoriteEntity = FavoriteEntity.builder()
                .userId("sender123")
                .nickName("새유저")
                .favorite(0)
                .favoriteHistoryEntityList(new ArrayList<>())
                .build();

        given(favoriteRepository.findById("sender123")).willReturn(Optional.of(zeroFavoriteEntity));

        // when
        favorite.run(chatDto);

        // then
        ArgumentCaptor<MessageRequestDto> messageCaptor = ArgumentCaptor.forClass(MessageRequestDto.class);
        BDDMockito.then(chzzkOpenApi).should().sendMessage(messageCaptor.capture());

        MessageRequestDto sentMessage = messageCaptor.getValue();
        then(sentMessage.getMessage()).contains("0 입니다.💛");
    }

    @Test
    void run_ShouldHandleHighFavorite() {
        // given
        FavoriteEntity highFavoriteEntity = FavoriteEntity.builder()
                .userId("sender123")
                .nickName("VIP유저")
                .favorite(99999)
                .favoriteHistoryEntityList(new ArrayList<>())
                .build();

        given(favoriteRepository.findById("sender123")).willReturn(Optional.of(highFavoriteEntity));

        // when
        favorite.run(chatDto);

        // then
        ArgumentCaptor<MessageRequestDto> messageCaptor = ArgumentCaptor.forClass(MessageRequestDto.class);
        BDDMockito.then(chzzkOpenApi).should().sendMessage(messageCaptor.capture());

        MessageRequestDto sentMessage = messageCaptor.getValue();
        then(sentMessage.getMessage()).isEqualTo("VIP유저님의 호감도는 99999 입니다.💛");
    }

    @Test
    void run_ShouldUseNicknameFromProfile_WhenCreatingNewUser() {
        // given
        ChatDto chatWithSpecialNickname = ChatDto.builder()
                .senderChannelId("newUser123")
                .content("!호감도")
                .profile(ChatDto.Profile.builder()
                        .nickname("특수문자@#유저")
                        .build())
                .build();

        FavoriteEntity newEntity = FavoriteEntity.builder()
                .userId("newUser123")
                .nickName("특수문자@#유저")
                .favorite(0)
                .favoriteHistoryEntityList(new ArrayList<>())
                .build();

        given(favoriteRepository.findById("newUser123")).willReturn(Optional.empty());
        given(favoriteRepository.save(any(FavoriteEntity.class))).willReturn(newEntity);

        // when
        favorite.run(chatWithSpecialNickname);

        // then
        ArgumentCaptor<FavoriteEntity> entityCaptor = ArgumentCaptor.forClass(FavoriteEntity.class);
        BDDMockito.then(favoriteRepository).should().save(entityCaptor.capture());

        FavoriteEntity savedEntity = entityCaptor.getValue();
        then(savedEntity.getNickName()).isEqualTo("특수문자@#유저");

        ArgumentCaptor<MessageRequestDto> messageCaptor = ArgumentCaptor.forClass(MessageRequestDto.class);
        BDDMockito.then(chzzkOpenApi).should().sendMessage(messageCaptor.capture());

        MessageRequestDto sentMessage = messageCaptor.getValue();
        then(sentMessage.getMessage()).contains("특수문자@#유저님의 호감도는");
    }

    @Test
    void run_ShouldHandleNegativeFavorite() {
        // given
        FavoriteEntity negativeFavoriteEntity = FavoriteEntity.builder()
                .userId("sender123")
                .nickName("제재유저")
                .favorite(-50)
                .favoriteHistoryEntityList(new ArrayList<>())
                .build();

        given(favoriteRepository.findById("sender123")).willReturn(Optional.of(negativeFavoriteEntity));

        // when
        favorite.run(chatDto);

        // then
        ArgumentCaptor<MessageRequestDto> messageCaptor = ArgumentCaptor.forClass(MessageRequestDto.class);
        BDDMockito.then(chzzkOpenApi).should().sendMessage(messageCaptor.capture());

        MessageRequestDto sentMessage = messageCaptor.getValue();
        then(sentMessage.getMessage()).contains("-50 입니다.💛");
    }

    @Test
    void run_ShouldUseSenderChannelIdAsUserId() {
        // given
        ChatDto multiUserChat = ChatDto.builder()
                .channelId("mainChannel")
                .senderChannelId("uniqueSender456")
                .content("!호감도")
                .profile(ChatDto.Profile.builder()
                        .nickname("유저456")
                        .build())
                .build();

        FavoriteEntity entity = FavoriteEntity.builder()
                .userId("uniqueSender456")
                .nickName("유저456")
                .favorite(75)
                .favoriteHistoryEntityList(new ArrayList<>())
                .build();

        given(favoriteRepository.findById("uniqueSender456")).willReturn(Optional.of(entity));

        // when
        favorite.run(multiUserChat);

        // then
        BDDMockito.then(favoriteRepository).should().findById("uniqueSender456");
        ArgumentCaptor<MessageRequestDto> messageCaptor = ArgumentCaptor.forClass(MessageRequestDto.class);
        BDDMockito.then(chzzkOpenApi).should().sendMessage(messageCaptor.capture());

        MessageRequestDto sentMessage = messageCaptor.getValue();
        then(sentMessage.getMessage()).isEqualTo("유저456님의 호감도는 75 입니다.💛");
    }
}
