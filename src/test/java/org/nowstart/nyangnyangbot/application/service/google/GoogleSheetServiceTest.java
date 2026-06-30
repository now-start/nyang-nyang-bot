package org.nowstart.nyangnyangbot.application.service.google;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
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
        // 준비
        doReturn(List.of(new GoogleSheetRow("새유저", "newUser", 10))).when(googleSheetService).getSheetValues();
        given(favoriteQueryPort.getOrCreate("newUser", "새유저"))
                .willReturn(new SummaryResult("newUser", "새유저", 0));

        // 실행
        googleSheetService.updateFavorite();

        // 검증
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
        // 준비
        doReturn(List.of(new GoogleSheetRow("기존닉네임", "user123", 70))).when(googleSheetService).getSheetValues();
        given(favoriteQueryPort.getOrCreate("user123", "기존닉네임")).willReturn(existingFavorite);

        // 실행
        googleSheetService.updateFavorite();

        // 검증
        BDDMockito.then(adjustFavoriteUseCase).should().adjust(argThat(command ->
                "user123".equals(command.userId())
                        && command.delta() == 20
                        && command.sourceType() == FavoriteSourceType.SHEET_MIGRATION
        ));
    }

    @Test
    void updateFavorite_ShouldNotUpdate_WhenFavoriteUnchanged() {
        // 준비
        SummaryResult unchangedEntity = new SummaryResult("user123", "기존닉네임", 50);
        doReturn(List.of(new GoogleSheetRow("기존닉네임", "user123", 50))).when(googleSheetService).getSheetValues();
        given(favoriteQueryPort.getOrCreate("user123", "기존닉네임")).willReturn(unchangedEntity);

        // 실행
        googleSheetService.updateFavorite();

        // 검증
        BDDMockito.then(adjustFavoriteUseCase).should(never()).adjust(any(AdjustFavoriteCommand.class));
        then(unchangedEntity.favorite()).isEqualTo(50);
    }

    @Test
    void updateFavorite_ShouldAddHistory_WhenFavoriteChanges() {
        // 준비
        SummaryResult entityWithHistory = new SummaryResult("user123", "유저", 100);
        doReturn(List.of(new GoogleSheetRow("유저", "user123", 120))).when(googleSheetService).getSheetValues();
        given(favoriteQueryPort.getOrCreate("user123", "유저")).willReturn(entityWithHistory);

        // 실행
        googleSheetService.updateFavorite();

        // 검증
        BDDMockito.then(adjustFavoriteUseCase).should().adjust(argThat(command ->
                "user123".equals(command.userId())
                        && "유저".equals(command.nickName())
                        && command.delta() == 20
                        && "데이터 동기화".equals(command.publicDescription())
        ));
    }

    @Test
    void updateFavorite_ShouldHandleDuplicateUsers_KeepingLatest() {
        // 실행
        List<GoogleSheetRow> rows = googleSheetService.normalizeRows(List.of(
                new GoogleSheetRow("예전닉네임", "user123", 30),
                new GoogleSheetRow("최신닉네임", "user123", 80)
        ));

        // 검증
        then(rows).containsExactly(new GoogleSheetRow("최신닉네임", "user123", 80));
    }

    @Test
    void updateFavorite_ShouldSkipEmptyUserIds() {
        // 실행
        List<GoogleSheetRow> rows = googleSheetService.normalizeRows(java.util.Arrays.asList(
                new GoogleSheetRow("빈값", "", 10),
                new GoogleSheetRow("정상", "user123", 20),
                null
        ));

        // 검증
        then(rows).containsExactly(new GoogleSheetRow("정상", "user123", 20));
    }

    @Test
    void updateFavorite_ShouldUpdateNickname_WhenChanged() {
        // 준비
        SummaryResult entity = new SummaryResult("user123", "이전닉네임", 100);
        doReturn(List.of(new GoogleSheetRow("새닉네임", "user123", 100))).when(googleSheetService).getSheetValues();
        given(favoriteQueryPort.getOrCreate("user123", "새닉네임")).willReturn(entity);

        // 실행
        googleSheetService.updateFavorite();

        // 검증
        BDDMockito.then(favoriteQueryPort).should().updateNickName("user123", "새닉네임");
        BDDMockito.then(adjustFavoriteUseCase).should(never()).adjust(any(AdjustFavoriteCommand.class));
    }

    @Test
    void updateFavorite_ShouldSkip_WhenSyncAlreadyInProgress() throws Exception {
        // 준비 - 첫 번째 동기화를 가드 안에서 멈춰두고, 그 사이 두 번째 호출을 시도한다.
        CountDownLatch firstEntered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        doAnswer(invocation -> {
            firstEntered.countDown();
            release.await();
            return List.<GoogleSheetRow>of();
        }).when(googleSheetService).getSheetValues();

        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            Future<?> first = pool.submit(() -> googleSheetService.updateFavorite());
            then(firstEntered.await(2, TimeUnit.SECONDS)).isTrue();

            // 실행 - 첫 동기화가 진행 중인 동안의 두 번째 호출은 즉시 건너뛰어야 한다.
            googleSheetService.updateFavorite();

            release.countDown();
            first.get(2, TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
        }

        // 검증 - 가드 덕분에 시트 조회는 첫 호출에서 단 한 번만 수행된다.
        BDDMockito.then(googleSheetService).should(times(1)).getSheetValues();
        BDDMockito.then(adjustFavoriteUseCase).should(never()).adjust(any(AdjustFavoriteCommand.class));
    }
}
