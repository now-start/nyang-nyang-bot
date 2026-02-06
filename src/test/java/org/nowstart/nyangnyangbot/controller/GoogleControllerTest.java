package org.nowstart.nyangnyangbot.controller;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.service.GoogleSheetService;

@ExtendWith(MockitoExtension.class)
class GoogleControllerTest {

    @Mock
    private GoogleSheetService googleSheetService;

    @InjectMocks
    private GoogleController googleController;

    @Test
    void syncDatabase_ShouldReturnSuccess_WhenUpdateSucceeds() {
        String result = googleController.syncDatabase();

        then(result).isEqualTo("SUCCESS");
        BDDMockito.then(googleSheetService).should().updateFavorite();
    }

    @Test
    void syncDatabase_ShouldThrow_WhenUpdateFails() {
        BDDMockito.willThrow(new RuntimeException("boom")).given(googleSheetService).updateFavorite();

        thenThrownBy(() -> googleController.syncDatabase())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("boom");
    }
}






