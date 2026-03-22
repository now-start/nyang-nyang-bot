package org.nowstart.nyangnyangbot.controller;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.BDDAssertions.then;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.service.GoogleSheetService;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class GoogleControllerTest {

    @Mock
    private GoogleSheetService googleSheetService;

    @InjectMocks
    private GoogleController googleController;

    @Test
    void syncDatabase_ShouldReturnSuccessResponse() {
        ResponseEntity<String> result = googleController.syncDatabase();

        then(result.getStatusCode().is2xxSuccessful()).isTrue();
        then(result.getBody()).isEqualTo("SUCCESS");
        BDDMockito.then(googleSheetService).should().updateFavorite();
    }

    @Test
    void syncDatabase_ShouldPropagateServiceException() {
        BDDMockito.willThrow(new IllegalStateException("sync failed"))
                .given(googleSheetService)
                .updateFavorite();

        assertThatThrownBy(() -> googleController.syncDatabase())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("sync failed");
    }
}
