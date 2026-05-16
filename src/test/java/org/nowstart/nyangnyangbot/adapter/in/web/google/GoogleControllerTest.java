package org.nowstart.nyangnyangbot.adapter.in.web.google;

import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import static org.assertj.core.api.BDDAssertions.then;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.application.service.google.GoogleSheetService;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class GoogleControllerTest {

    @Mock
    private GoogleSheetService googleSheetService;

    @InjectMocks
    private GoogleController googleController;

    @Test
    void syncDatabase_ShouldReturnSuccessResponse() {
        // 실행
        ResponseEntity<String> result = googleController.syncDatabase();

        // 검증
        then(result.getStatusCode().is2xxSuccessful()).isTrue();
        then(result.getBody()).isEqualTo("SUCCESS");
        BDDMockito.then(googleSheetService).should().updateFavorite();
    }

    @Test
    void syncDatabase_ShouldPropagateServiceException() {
        // 준비
        BDDMockito.willThrow(new IllegalStateException("sync failed"))
                .given(googleSheetService)
                .updateFavorite();

        // 실행 및 검증
        thenThrownBy(() -> googleController.syncDatabase())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("sync failed");
    }
}
