package org.nowstart.nyangnyangbot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.application.favorite.AdjustFavoriteCommand;
import org.nowstart.nyangnyangbot.application.favorite.AdjustFavoriteUseCase;
import org.nowstart.nyangnyangbot.data.dto.sheet.GoogleSheetDto;
import org.nowstart.nyangnyangbot.data.entity.FavoriteEntity;
import org.nowstart.nyangnyangbot.data.property.GoogleProperty;
import org.nowstart.nyangnyangbot.domain.favorite.FavoriteSourceType;
import org.nowstart.nyangnyangbot.repository.FavoriteRepository;

@ExtendWith(MockitoExtension.class)
class GoogleSheetServiceTest {

    @Mock
    private GoogleProperty googleProperty;

    @Mock
    private FavoriteRepository favoriteRepository;

    @Mock
    private AdjustFavoriteUseCase adjustFavoriteUseCase;

    @Spy
    @InjectMocks
    private GoogleSheetService googleSheetService;

    private FavoriteEntity existingFavorite;

    @BeforeEach
    void setUp() {
        existingFavorite =
                FavoriteEntity.builder().userId("user123").nickName("기존닉네임").favorite(50).build();
    }

    @Test
    void updateFavorite_ShouldCreateNewEntity_WhenUserNotExists() {
        FavoriteEntity newEntity = FavoriteEntity.builder().userId("newUser").nickName("새유저").favorite(0).build();
        doReturn(List.of(new GoogleSheetDto("새유저", "newUser", 10))).when(googleSheetService).getSheetValues();
        given(favoriteRepository.findById("newUser")).willReturn(Optional.empty());
        given(favoriteRepository.save(any(FavoriteEntity.class))).willReturn(newEntity);

        googleSheetService.updateFavorite();

        BDDMockito.then(favoriteRepository).should().findById("newUser");
        BDDMockito.then(adjustFavoriteUseCase).should().adjust(argThat(command ->
                "newUser".equals(command.userId())
                        && "새유저".equals(command.nickName())
                        && command.delta() == 10
                        && command.sourceType() == FavoriteSourceType.SHEET_MIGRATION
                        && "데이터 동기화".equals(command.publicDescription())
                        && command.createIfMissing()
        ));
    }

    @Test
    void updateFavorite_ShouldUpdateExistingEntity_WhenFavoriteChanged() {
        doReturn(List.of(new GoogleSheetDto("기존닉네임", "user123", 70))).when(googleSheetService).getSheetValues();
        given(favoriteRepository.findById("user123")).willReturn(Optional.of(existingFavorite));

        googleSheetService.updateFavorite();

        BDDMockito.then(adjustFavoriteUseCase).should().adjust(argThat(command ->
                "user123".equals(command.userId())
                        && command.delta() == 20
                        && command.sourceType() == FavoriteSourceType.SHEET_MIGRATION
        ));
    }

    @Test
    void updateFavorite_ShouldNotUpdate_WhenFavoriteUnchanged() {
        FavoriteEntity unchangedEntity =
                FavoriteEntity.builder().userId("user123").nickName("기존닉네임").favorite(50).build();
        doReturn(List.of(new GoogleSheetDto("기존닉네임", "user123", 50))).when(googleSheetService).getSheetValues();
        given(favoriteRepository.findById("user123")).willReturn(Optional.of(unchangedEntity));

        googleSheetService.updateFavorite();

        BDDMockito.then(adjustFavoriteUseCase).should(never()).adjust(any(AdjustFavoriteCommand.class));
        assertThat(unchangedEntity.getFavorite()).isEqualTo(50);
    }

    @Test
    void updateFavorite_ShouldAddHistory_WhenFavoriteChanges() {
        FavoriteEntity entityWithHistory =
                FavoriteEntity.builder().userId("user123").nickName("유저").favorite(100).build();
        doReturn(List.of(new GoogleSheetDto("유저", "user123", 120))).when(googleSheetService).getSheetValues();
        given(favoriteRepository.findById("user123")).willReturn(Optional.of(entityWithHistory));

        googleSheetService.updateFavorite();

        BDDMockito.then(adjustFavoriteUseCase).should().adjust(argThat(command ->
                "user123".equals(command.userId())
                        && "유저".equals(command.nickName())
                        && command.delta() == 20
                        && "데이터 동기화".equals(command.publicDescription())
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
        FavoriteEntity entity =
                FavoriteEntity.builder().userId("user123").nickName("이전닉네임").favorite(100).build();
        doReturn(List.of(new GoogleSheetDto("새닉네임", "user123", 100))).when(googleSheetService).getSheetValues();
        given(favoriteRepository.findById("user123")).willReturn(Optional.of(entity));

        googleSheetService.updateFavorite();

        assertThat(entity.getNickName()).isEqualTo("새닉네임");
        BDDMockito.then(adjustFavoriteUseCase).should(never()).adjust(any(AdjustFavoriteCommand.class));
    }
}
