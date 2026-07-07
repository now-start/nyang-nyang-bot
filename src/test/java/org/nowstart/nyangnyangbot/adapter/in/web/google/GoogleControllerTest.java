package org.nowstart.nyangnyangbot.adapter.in.web.google;

import static org.assertj.core.api.BDDAssertions.then;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.application.port.in.google.SyncGoogleSheetUseCase;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;

@ExtendWith(MockitoExtension.class)
class GoogleControllerTest {

    @Mock
    private SyncGoogleSheetUseCase syncGoogleSheetUseCase;

    @InjectMocks
    private GoogleController googleController;

    @Test
    @DisplayName("동기화 요청 시 성공 alert fragment와 board refresh trigger를 반환한다")
    void syncDatabase_ShouldReturnSuccessAlertFragment() {
        // 준비
        MockHttpServletResponse response = new MockHttpServletResponse();
        ExtendedModelMap model = new ExtendedModelMap();

        // 실행
        String view = googleController.syncDatabase(response, model);

        // 검증
        then(view).isEqualTo("components/feedback :: alert");
        then(model.get("message")).isEqualTo("데이터 동기화 완료");
        then(model.get("tone")).isEqualTo("success");
        then(response.getHeader("HX-Trigger")).isEqualTo("favorite-board-refresh");
        BDDMockito.then(syncGoogleSheetUseCase).should().updateFavorite();
    }

    @Test
    @DisplayName("서비스 예외를 실패 alert fragment로 변환한다")
    void syncDatabase_ShouldReturnFailureAlertFragment_WhenServiceFails() {
        // 준비
        BDDMockito.willThrow(new IllegalStateException("sync failed"))
                .given(syncGoogleSheetUseCase)
                .updateFavorite();
        MockHttpServletResponse response = new MockHttpServletResponse();
        ExtendedModelMap model = new ExtendedModelMap();

        // 실행
        String view = googleController.syncDatabase(response, model);

        // 검증
        then(view).isEqualTo("components/feedback :: alert");
        then(model.get("message")).isEqualTo("데이터 동기화 실패");
        then(model.get("tone")).isEqualTo("danger");
        then(response.getHeader("HX-Trigger")).isNull();
    }

    @Test
    @DisplayName("컨트롤러 동기화 API는 스케줄러 책임을 갖지 않는다")
    void syncDatabase_ShouldNotHaveScheduledAnnotation() throws NoSuchMethodException {
        // 실행
        boolean scheduled = GoogleController.class
                .getMethod("syncDatabase", HttpServletResponse.class, Model.class)
                .isAnnotationPresent(Scheduled.class);

        // 검증
        then(scheduled).isFalse();
    }

    @Test
    @DisplayName("데이터베이스 동기화 API는 POST로만 노출한다")
    void syncDatabase_ShouldUsePostMapping() throws NoSuchMethodException {
        // 실행
        PostMapping postMapping = GoogleController.class
                .getMethod("syncDatabase", HttpServletResponse.class, Model.class)
                .getAnnotation(PostMapping.class);

        // 검증
        then(postMapping).isNotNull();
        then(postMapping.value()).containsExactly("/sync");
    }
}
