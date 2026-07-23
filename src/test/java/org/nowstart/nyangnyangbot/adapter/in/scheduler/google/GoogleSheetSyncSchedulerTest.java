package org.nowstart.nyangnyangbot.adapter.in.scheduler.google;

import static org.assertj.core.api.BDDAssertions.then;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.application.port.in.google.SyncGoogleSheetUseCase;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;

@ExtendWith(MockitoExtension.class)
class GoogleSheetSyncSchedulerTest {

    @Mock
    private SyncGoogleSheetUseCase syncGoogleSheetUseCase;

    @InjectMocks
    private GoogleSheetSyncScheduler googleSheetSyncScheduler;

    @Test
    @DisplayName("스케줄 실행 시 구글 시트 동기화 유스케이스를 호출한다")
    void syncDatabase_ShouldCallGoogleSheetSyncUseCase() {
        // 실행
        googleSheetSyncScheduler.syncDatabase();

        // 검증
        BDDMockito.then(syncGoogleSheetUseCase).should().synchronizePoints();
    }

    @Test
    @DisplayName("구글 시트 동기화는 매일 새벽 4시에 실행된다")
    void syncDatabase_ShouldRunAtFourEveryDay() throws NoSuchMethodException {
        // 실행
        Scheduled scheduled = GoogleSheetSyncScheduler.class
                .getMethod("syncDatabase")
                .getAnnotation(Scheduled.class);

        // 검증
        then(scheduled).isNotNull();
        then(scheduled.cron()).isEqualTo("0 0 4 * * ?");
    }

    @Test
    @DisplayName("구글 시트 동기화 스케줄러는 설정이 없으면 기본 활성화된다")
    void scheduler_ShouldBeEnabledByDefault() {
        // 실행
        ConditionalOnProperty conditionalOnProperty =
                GoogleSheetSyncScheduler.class.getAnnotation(ConditionalOnProperty.class);

        // 검증
        then(conditionalOnProperty).isNotNull();
        then(conditionalOnProperty.prefix()).isEqualTo("google.spreadsheet.sync");
        then(conditionalOnProperty.name()).containsExactly("enabled");
        then(conditionalOnProperty.havingValue()).isEqualTo("true");
        then(conditionalOnProperty.matchIfMissing()).isTrue();
    }
}
