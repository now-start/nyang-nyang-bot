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
import org.nowstart.nyangnyangbot.application.model.FavoriteSummary;
import org.nowstart.nyangnyangbot.application.port.out.favorite.FavoriteQueryPort;
import org.nowstart.nyangnyangbot.data.dto.sheet.GoogleSheetDto;
import org.nowstart.nyangnyangbot.data.property.GoogleProperty;
import org.nowstart.nyangnyangbot.domain.favorite.FavoriteSourceType;

@ExtendWith(MockitoExtension.class)
class GoogleSheetServiceTest {

    @Mock
    private GoogleProperty googleProperty;

    @Mock
    private FavoriteQueryPort favoriteQueryPort;

    @Mock
    private AdjustFavoriteUseCase adjustFavoriteUseCase;

    @Spy
    @InjectMocks
    private GoogleSheetService googleSheetService;

    private FavoriteSummary existingFavorite;

    @BeforeEach
    void setUp() {
        existingFavorite = new FavoriteSummary("user123", "기존닉네임", 50);
    }

    @Test
    void updateFavorite_ShouldCreateNewEntity_WhenUserNotExists() {
        doReturn(List.of(new GoogleSheetDto("새유저", "newUser", 10))).when(googleSheetService).getSheetValues();
        given(favoriteQueryPort.getOrCreate("newUser", "새유저"))
                .willReturn(new FavoriteSummary("newUser", "새유저", 0));

        googleSheetService.updateFavorite();

        BDDMockito.then(favoriteQueryPort).should().getOrCreate("newUser", "새유저");
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
        given(favoriteQueryPort.getOrCreate("user123", "기존닉네임")).willReturn(existingFavorite);

        googleSheetService.updateFavorite();

        BDDMockito.then(adjustFavoriteUseCase).should().adjust(argThat(command ->
                "user123".equals(command.userId())
                        && command.delta() == 20
                        && command.sourceType() == FavoriteSourceType.SHEET_MIGRATION
        ));
    }

    @Test
    void updateFavorite_ShouldNotUpdate_WhenFavoriteUnchanged() {
        FavoriteSummary unchangedEntity = new FavoriteSummary("user123", "기존닉네임", 50);
        doReturn(List.of(new GoogleSheetDto("기존닉네임", "user123", 50))).when(googleSheetService).getSheetValues();
        given(favoriteQueryPort.getOrCreate("user123", "기존닉네임")).willReturn(unchangedEntity);

        googleSheetService.updateFavorite();

        BDDMockito.then(adjustFavoriteUseCase).should(never()).adjust(any(AdjustFavoriteCommand.class));
        assertThat(unchangedEntity.favorite()).isEqualTo(50);
    }

    @Test
    void updateFavorite_ShouldAddHistory_WhenFavoriteChanges() {
        FavoriteSummary entityWithHistory = new FavoriteSummary("user123", "유저", 100);
        doReturn(List.of(new GoogleSheetDto("유저", "user123", 120))).when(googleSheetService).getSheetValues();
        given(favoriteQueryPort.getOrCreate("user123", "유저")).willReturn(entityWithHistory);

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
        FavoriteSummary entity = new FavoriteSummary("user123", "이전닉네임", 100);
        doReturn(List.of(new GoogleSheetDto("새닉네임", "user123", 100))).when(googleSheetService).getSheetValues();
        given(favoriteQueryPort.getOrCreate("user123", "새닉네임")).willReturn(entity);

        googleSheetService.updateFavorite();

        BDDMockito.then(favoriteQueryPort).should().updateNickName("user123", "새닉네임");
        BDDMockito.then(adjustFavoriteUseCase).should(never()).adjust(any(AdjustFavoriteCommand.class));
    }
}
