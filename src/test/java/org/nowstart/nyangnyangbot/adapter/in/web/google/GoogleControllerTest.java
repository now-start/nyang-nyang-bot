package org.nowstart.nyangnyangbot.adapter.in.web.google;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.application.port.in.google.SyncGoogleSheetUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;

@ExtendWith(MockitoExtension.class)
class GoogleControllerTest {

    @Mock
    private SyncGoogleSheetUseCase syncGoogleSheetUseCase;

    @InjectMocks
    private GoogleController googleController;

    @Test
    @DisplayName("데이터베이스 동기화 요청 시 SUCCESS 응답을 반환한다")
    void syncDatabase_ShouldReturnSuccessResponse() {
        // 실행
        ResponseEntity<String> result = googleController.syncDatabase();

        // 검증
        then(result.getStatusCode().is2xxSuccessful()).isTrue();
        then(result.getBody()).isEqualTo("SUCCESS");
        BDDMockito.then(syncGoogleSheetUseCase).should().updateFavorite();
    }

    @Test
    @DisplayName("서비스에서 예외가 발생하면 예외를 전파한다")
    void syncDatabase_ShouldPropagateServiceException() {
        // 준비
        BDDMockito.willThrow(new IllegalStateException("sync failed"))
                .given(syncGoogleSheetUseCase)
                .updateFavorite();

        // 실행 및 검증
        thenThrownBy(() -> googleController.syncDatabase())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("sync failed");
    }

    @Test
    @DisplayName("컨트롤러 동기화 API는 스케줄러 책임을 갖지 않는다")
    void syncDatabase_ShouldNotHaveScheduledAnnotation() throws NoSuchMethodException {
        // 실행
        boolean scheduled = GoogleController.class
                .getMethod("syncDatabase")
                .isAnnotationPresent(Scheduled.class);

        // 검증
        then(scheduled).isFalse();
    }
}
