package org.nowstart.nyangnyangbot.application.service.google;

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
import org.nowstart.nyangnyangbot.application.port.in.favorite.AdjustFavoriteUseCase.AdjustFavoriteCommand;
import org.nowstart.nyangnyangbot.application.port.in.favorite.AdjustFavoriteUseCase;
import org.nowstart.nyangnyangbot.application.port.out.favorite.FavoriteQueryPort.SummaryResult;
import org.nowstart.nyangnyangbot.application.port.out.favorite.FavoriteQueryPort;
import org.nowstart.nyangnyangbot.application.port.out.google.GoogleSheetPort;
import org.nowstart.nyangnyangbot.application.port.out.google.GoogleSheetPort.GoogleSheetRow;
import org.nowstart.nyangnyangbot.domain.favorite.FavoriteSourceType;

@ExtendWith(MockitoExtension.class)
class GoogleSheetServiceTest {

    @Mock
    private FavoriteQueryPort favoriteQueryPort;

    @Mock
    private AdjustFavoriteUseCase adjustFavoriteUseCase;

    @Mock
    private GoogleSheetPort googleSheetPort;

    @Spy
    @InjectMocks
    private GoogleSheetService googleSheetService;

    private SummaryResult existingFavorite;

    @BeforeEach
    void setUp() {
        existingFavorite = new SummaryResult("user123", "기존닉네임", 50);
    }

    @Test
    void updateFavorite_ShouldCreateNewEntity_WhenUserNotExists() {
        doReturn(List.of(new GoogleSheetRow("새유저", "newUser", 10))).when(googleSheetService).getSheetValues();
        given(favoriteQueryPort.getOrCreate("newUser", "새유저"))
                .willReturn(new SummaryResult("newUser", "새유저", 0));

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
        doReturn(List.of(new GoogleSheetRow("기존닉네임", "user123", 70))).when(googleSheetService).getSheetValues();
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
        SummaryResult unchangedEntity = new SummaryResult("user123", "기존닉네임", 50);
        doReturn(List.of(new GoogleSheetRow("기존닉네임", "user123", 50))).when(googleSheetService).getSheetValues();
        given(favoriteQueryPort.getOrCreate("user123", "기존닉네임")).willReturn(unchangedEntity);

        googleSheetService.updateFavorite();

        BDDMockito.then(adjustFavoriteUseCase).should(never()).adjust(any(AdjustFavoriteCommand.class));
        assertThat(unchangedEntity.favorite()).isEqualTo(50);
    }

    @Test
    void updateFavorite_ShouldAddHistory_WhenFavoriteChanges() {
        SummaryResult entityWithHistory = new SummaryResult("user123", "유저", 100);
        doReturn(List.of(new GoogleSheetRow("유저", "user123", 120))).when(googleSheetService).getSheetValues();
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
        List<GoogleSheetRow> rows = googleSheetService.normalizeRows(List.of(
                new GoogleSheetRow("예전닉네임", "user123", 30),
                new GoogleSheetRow("최신닉네임", "user123", 80)
        ));

        assertThat(rows).containsExactly(new GoogleSheetRow("최신닉네임", "user123", 80));
    }

    @Test
    void updateFavorite_ShouldSkipEmptyUserIds() {
        List<GoogleSheetRow> rows = googleSheetService.normalizeRows(java.util.Arrays.asList(
                new GoogleSheetRow("빈값", "", 10),
                new GoogleSheetRow("정상", "user123", 20),
                null
        ));

        assertThat(rows).containsExactly(new GoogleSheetRow("정상", "user123", 20));
    }

    @Test
    void updateFavorite_ShouldUpdateNickname_WhenChanged() {
        SummaryResult entity = new SummaryResult("user123", "이전닉네임", 100);
        doReturn(List.of(new GoogleSheetRow("새닉네임", "user123", 100))).when(googleSheetService).getSheetValues();
        given(favoriteQueryPort.getOrCreate("user123", "새닉네임")).willReturn(entity);

        googleSheetService.updateFavorite();

        BDDMockito.then(favoriteQueryPort).should().updateNickName("user123", "새닉네임");
        BDDMockito.then(adjustFavoriteUseCase).should(never()).adjust(any(AdjustFavoriteCommand.class));
    }
}
