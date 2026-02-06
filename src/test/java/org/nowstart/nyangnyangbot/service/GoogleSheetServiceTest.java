package org.nowstart.nyangnyangbot.service;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.data.entity.FavoriteEntity;
import org.nowstart.nyangnyangbot.repository.FavoriteHistoryRepository;
import org.nowstart.nyangnyangbot.repository.FavoriteRepository;

@ExtendWith(MockitoExtension.class)
class GoogleSheetServiceTest {

    @Mock
    private DefaultGoogleSheetValuesProvider valuesProvider;

    @Mock
    private FavoriteRepository favoriteRepository;

    @Mock
    private FavoriteHistoryRepository favoriteHistoryRepository;

    @InjectMocks
    private GoogleSheetService googleSheetService;

    @Test
    void updateFavorite_ShouldSkipInvalidRows() {
        List<List<Object>> rows = List.of(
                List.of("nick", " ", 1),
                List.of("nick2")
        );
        given(valuesProvider.fetchValues()).willReturn(rows);

        googleSheetService.updateFavorite();

        BDDMockito.then(favoriteRepository).shouldHaveNoInteractions();
    }

    @Test
    void updateFavorite_ShouldCreateAndUpdate_WhenMissing() {
        List<List<Object>> rows = List.of(
                List.of("nick", "user1", 3)
        );
        FavoriteEntity saved = FavoriteEntity.builder()
                .userId("user1")
                .nickName("nick")
                .favorite(0)
                .build();

        given(valuesProvider.fetchValues()).willReturn(rows);
        given(favoriteRepository.findById("user1")).willReturn(Optional.empty());
        given(favoriteRepository.save(BDDMockito.any(FavoriteEntity.class))).willReturn(saved);

        googleSheetService.updateFavorite();

        then(saved.getFavorite()).isEqualTo(3);
        then(saved.getNickName()).isEqualTo("nick");
        then(saved.getFavoriteHistoryEntityList()).hasSize(1);
        then(saved.getFavoriteHistoryEntityList().get(0).getFavorite()).isEqualTo(3);
    }

    @Test
    void updateFavorite_ShouldNotUpdate_WhenFavoriteUnchanged() {
        List<List<Object>> rows = List.of(
                List.of("nick", "user2", 5)
        );
        FavoriteEntity existing = FavoriteEntity.builder()
                .userId("user2")
                .nickName("nick")
                .favorite(5)
                .favoriteHistoryEntityList(new ArrayList<>())
                .build();

        given(valuesProvider.fetchValues()).willReturn(rows);
        given(favoriteRepository.findById("user2")).willReturn(Optional.of(existing));

        googleSheetService.updateFavorite();

        then(existing.getFavoriteHistoryEntityList()).isEmpty();
        BDDMockito.then(favoriteRepository).should(never()).save(BDDMockito.any(FavoriteEntity.class));
    }

    @Test
    void updateFavorite_ShouldUpdateAndAddHistory_WhenFavoriteChanged() {
        List<List<Object>> rows = List.of(
                List.of("newnick", "user3", 7)
        );
        FavoriteEntity existing = FavoriteEntity.builder()
                .userId("user3")
                .nickName("oldnick")
                .favorite(1)
                .favoriteHistoryEntityList(new ArrayList<>())
                .build();

        given(valuesProvider.fetchValues()).willReturn(rows);
        given(favoriteRepository.findById("user3")).willReturn(Optional.of(existing));

        googleSheetService.updateFavorite();

        then(existing.getNickName()).isEqualTo("newnick");
        then(existing.getFavorite()).isEqualTo(7);
        then(existing.getFavoriteHistoryEntityList()).hasSize(1);
    }

    @Test
    void updateFavorite_ShouldUseLatest_WhenDuplicateUserIds() {
        List<List<Object>> rows = List.of(
                List.of("first", "user4", 1),
                List.of("second", "user4", 4)
        );
        FavoriteEntity existing = FavoriteEntity.builder()
                .userId("user4")
                .nickName("start")
                .favorite(0)
                .favoriteHistoryEntityList(new ArrayList<>())
                .build();

        given(valuesProvider.fetchValues()).willReturn(rows);
        given(favoriteRepository.findById("user4")).willReturn(Optional.of(existing));

        googleSheetService.updateFavorite();

        then(existing.getNickName()).isEqualTo("second");
        then(existing.getFavorite()).isEqualTo(4);
        BDDMockito.then(favoriteRepository).should(times(1)).findById("user4");
    }
}






