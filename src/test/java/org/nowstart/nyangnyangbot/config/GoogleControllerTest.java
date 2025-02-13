package org.nowstart.nyangnyangbot.config;

import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.nowstart.nyangnyangbot.controller.GoogleController;
import org.nowstart.nyangnyangbot.data.dto.GoogleSheetDto;
import org.nowstart.nyangnyangbot.service.GoogleSheetService;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
class GoogleControllerTest {

    @InjectMocks
    GoogleController googleController;
    @Mock
    GoogleSheetService googleSheetService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(googleController, "credentialsFilePath", "./key/google_spread_sheet_key.json");
        ReflectionTestUtils.setField(googleController, "spreadSheetId", "1PKgmtFVrJWw4briZGxlfyKKUd3XaQscsmAGR6LZ12Os");
    }

    @Test
    void getSheetValuesTest() throws Exception {
        //given
        Method method = googleController.getClass().getDeclaredMethod("getSheetValues");
        method.setAccessible(true);

        //when
        List<GoogleSheetDto> list = (List<GoogleSheetDto>) method.invoke(googleController);

        //then
        assertThat(list).isNotEmpty();
    }
}