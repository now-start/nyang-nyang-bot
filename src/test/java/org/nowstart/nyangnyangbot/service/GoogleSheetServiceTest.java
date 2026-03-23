package org.nowstart.nyangnyangbot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.data.dto.sheet.GoogleSheetDto;
import org.nowstart.nyangnyangbot.data.entity.FavoriteEntity;
import org.nowstart.nyangnyangbot.data.entity.FavoriteHistoryEntity;
import org.nowstart.nyangnyangbot.data.entity.UserKarmaEntity;
import org.nowstart.nyangnyangbot.data.property.GoogleProperty;
import org.nowstart.nyangnyangbot.data.type.FavoriteHistoryType;
import org.nowstart.nyangnyangbot.data.type.FavoriteKarmaSourceType;
import org.nowstart.nyangnyangbot.repository.FavoriteHistoryRepository;
import org.nowstart.nyangnyangbot.repository.FavoriteRepository;
import org.nowstart.nyangnyangbot.repository.UserKarmaRepository;

@ExtendWith(MockitoExtension.class)
class GoogleSheetServiceTest {

    @Mock
    private GoogleProperty googleProperty;

    @Mock
    private FavoriteRepository favoriteRepository;

    private final List<UserKarmaEntity> storedKarmas = new ArrayList<>();

    @Mock
    private FavoriteHistoryRepository favoriteHistoryRepository;
    @Mock
    private UserKarmaRepository userKarmaRepository;
    private GoogleSheetService googleSheetService;
    private FavoriteAggregationService favoriteAggregationService;

    private FavoriteEntity existingFavorite;

    @BeforeEach
    void setUp() {
        storedKarmas.clear();
        favoriteAggregationService = new FavoriteAggregationService(favoriteRepository, userKarmaRepository);
        googleSheetService = spy(new GoogleSheetService(
                googleProperty,
                favoriteRepository,
                userKarmaRepository,
                favoriteHistoryRepository,
                favoriteAggregationService
        ));
        stubKarmaRepository();

        existingFavorite = favoriteEntity("user123", "기존닉네임", 50, 5, 15);
    }

    @Test
    void updateFavorite_ShouldCreateNewEntity_WhenUserNotExists() {
        FavoriteEntity newEntity = favoriteEntity("newUser", "새유저", 0, 0, 0);
        doReturn(List.of(new GoogleSheetDto("새유저", "newUser", 10))).when(googleSheetService).getSheetValues();
        given(favoriteRepository.findById("newUser")).willReturn(Optional.empty());
        given(favoriteRepository.save(any(FavoriteEntity.class))).willReturn(newEntity);

        googleSheetService.updateFavorite();

        BDDMockito.then(favoriteRepository).should().findById("newUser");
        BDDMockito.then(userKarmaRepository).should().deleteAllByFavoriteEntityUserIdAndSourceType("newUser", FavoriteKarmaSourceType.SYNC);
        BDDMockito.then(userKarmaRepository).should().save(argThat(karma ->
                karma.getFavoriteEntity() == newEntity
                        && Integer.valueOf(10).equals(karma.getAmount())
                        && Integer.valueOf(1).equals(karma.getQuantity())
                        && FavoriteKarmaSourceType.SYNC.equals(karma.getSourceType())
        ));
        BDDMockito.then(favoriteHistoryRepository).should().save(argThat(history ->
                history.getFavoriteEntity() == newEntity
                        && Integer.valueOf(10).equals(history.getFavorite())
                        && Integer.valueOf(10).equals(history.getKarmaScore())
                        && Integer.valueOf(0).equals(history.getAttendanceCount())
                        && FavoriteHistoryType.SYNC.equals(history.getType())
                        && "데이터 동기화".equals(history.getHistory())
        ));
        assertThat(newEntity.getFavorite()).isEqualTo(10);
        assertThat(newEntity.getTotalFavorite()).isEqualTo(10);
    }

    @Test
    void updateFavorite_ShouldUpdateExistingEntity_WhenFavoriteChanged() {
        storedKarmas.add(userKarma(existingFavorite, 1, 15, FavoriteKarmaSourceType.ADJUSTMENT));
        doReturn(List.of(new GoogleSheetDto("기존닉네임", "user123", 70))).when(googleSheetService).getSheetValues();
        given(favoriteRepository.findById("user123")).willReturn(Optional.of(existingFavorite));

        googleSheetService.updateFavorite();

        assertThat(existingFavorite.getFavorite()).isEqualTo(70);
        assertThat(existingFavorite.getTotalFavorite()).isEqualTo(70);
        BDDMockito.then(userKarmaRepository).should().save(argThat(karma ->
                karma.getFavoriteEntity() == existingFavorite
                        && Integer.valueOf(50).equals(karma.getAmount())
                        && Integer.valueOf(1).equals(karma.getQuantity())
                        && FavoriteKarmaSourceType.SYNC.equals(karma.getSourceType())
        ));
        BDDMockito.then(favoriteHistoryRepository).should().save(argThat(history ->
                history.getFavoriteEntity() == existingFavorite
                        && Integer.valueOf(70).equals(history.getFavorite())
                        && Integer.valueOf(65).equals(history.getKarmaScore())
                        && Integer.valueOf(5).equals(history.getAttendanceCount())
                        && FavoriteHistoryType.SYNC.equals(history.getType())
        ));
    }

    @Test
    void updateFavorite_ShouldNotUpdate_WhenFavoriteUnchanged() {
        FavoriteEntity unchangedEntity = favoriteEntity("user123", "기존닉네임", 50, 0, 0);
        doReturn(List.of(new GoogleSheetDto("기존닉네임", "user123", 50))).when(googleSheetService).getSheetValues();
        given(favoriteRepository.findById("user123")).willReturn(Optional.of(unchangedEntity));

        googleSheetService.updateFavorite();

        BDDMockito.then(favoriteHistoryRepository).should(never()).save(any(FavoriteHistoryEntity.class));
        assertThat(unchangedEntity.getFavorite()).isEqualTo(50);
        assertThat(unchangedEntity.getTotalFavorite()).isEqualTo(50);
    }

    @Test
    void updateFavorite_ShouldAddHistory_WhenFavoriteChanges() {
        FavoriteEntity entityWithHistory = favoriteEntity("user123", "유저", 100, 0, 0);
        doReturn(List.of(new GoogleSheetDto("유저", "user123", 120))).when(googleSheetService).getSheetValues();
        given(favoriteRepository.findById("user123")).willReturn(Optional.of(entityWithHistory));

        googleSheetService.updateFavorite();

        BDDMockito.then(favoriteHistoryRepository).should().save(argThat(history ->
                history.getFavoriteEntity() == entityWithHistory
                        && Integer.valueOf(120).equals(history.getFavorite())
                        && Integer.valueOf(120).equals(history.getKarmaScore())
                        && Integer.valueOf(0).equals(history.getAttendanceCount())
                        && FavoriteHistoryType.SYNC.equals(history.getType())
                        && "데이터 동기화".equals(history.getHistory())
        ));
    }

    @Test
    void updateFavorite_ShouldHandleDuplicateUsers_KeepingLatest() {
        List<GoogleSheetDto> rows = googleSheetService.normalizeRows(List.of(
                new GoogleSheetDto("예전닉네임", "user123", 30),
                new GoogleSheetDto("최신닉네임", "user123", 80)
        ));

        assertThat(rows).containsExactly(new GoogleSheetDto("최신닉네임", "user123", 80));
    }

    @Test
    void updateFavorite_ShouldSkipEmptyUserIds() {
        List<GoogleSheetDto> rows = googleSheetService.normalizeRows(java.util.Arrays.asList(
                new GoogleSheetDto("빈값", "", 10),
                new GoogleSheetDto("정상", "user123", 20),
                null
        ));

        assertThat(rows).containsExactly(new GoogleSheetDto("정상", "user123", 20));
    }

    @Test
    void updateFavorite_ShouldUpdateNickname_WhenChanged() {
        FavoriteEntity entity = favoriteEntity("user123", "이전닉네임", 100, 0, 0);
        doReturn(List.of(new GoogleSheetDto("새닉네임", "user123", 100))).when(googleSheetService).getSheetValues();
        given(favoriteRepository.findById("user123")).willReturn(Optional.of(entity));

        googleSheetService.updateFavorite();

        assertThat(entity.getNickName()).isEqualTo("새닉네임");
        BDDMockito.then(favoriteHistoryRepository).should(never()).save(any(FavoriteHistoryEntity.class));
    }

    private void stubKarmaRepository() {
        lenient().doAnswer(invocation -> {
            UserKarmaEntity karma = invocation.getArgument(0);
            storedKarmas.add(karma);
            return karma;
        }).when(userKarmaRepository).save(any(UserKarmaEntity.class));

        lenient().doAnswer(invocation -> {
            String userId = invocation.getArgument(0);
            FavoriteKarmaSourceType sourceType = invocation.getArgument(1);
            storedKarmas.removeIf(karma ->
                    karma.getFavoriteEntity() != null
                            && userId.equals(karma.getFavoriteEntity().getUserId())
                            && sourceType.equals(karma.getSourceType())
            );
            return null;
        }).when(userKarmaRepository).deleteAllByFavoriteEntityUserIdAndSourceType(anyString(), any(FavoriteKarmaSourceType.class));

        lenient().when(userKarmaRepository.findAllByFavoriteEntityUserId(anyString())).thenAnswer(invocation -> {
            String userId = invocation.getArgument(0);
            return storedKarmas.stream()
                    .filter(karma -> karma.getFavoriteEntity() != null && userId.equals(karma.getFavoriteEntity().getUserId()))
                    .toList();
        });
    }

    private FavoriteEntity favoriteEntity(String userId, String nickName, int favorite, int attendanceCount, int karmaScore) {
        FavoriteEntity favoriteEntity = FavoriteEntity.builder()
                .userId(userId)
                .nickName(nickName)
                .totalFavorite(favorite)
                .build();
        favoriteEntity.setAttendanceCount(attendanceCount);
        favoriteEntity.setKarmaScore(karmaScore);
        favoriteEntity.setTotalFavorite(favorite);
        return favoriteEntity;
    }

    private UserKarmaEntity userKarma(
            FavoriteEntity favoriteEntity,
            Integer quantity,
            Integer amount,
            FavoriteKarmaSourceType sourceType
    ) {
        return UserKarmaEntity.builder()
                .favoriteEntity(favoriteEntity)
                .quantity(quantity)
                .amount(amount)
                .sourceType(sourceType)
                .label(sourceType.name())
                .build();
    }
}
