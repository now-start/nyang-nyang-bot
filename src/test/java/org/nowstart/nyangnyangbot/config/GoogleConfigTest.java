package org.nowstart.nyangnyangbot.config;

import io.micrometer.common.util.StringUtils;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.nowstart.nyangnyangbot.data.dto.GoogleSheetDto;
import org.nowstart.nyangnyangbot.service.GoogleSheetService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
class GoogleConfigTest {

    @InjectMocks
    GoogleConfig googleConfig;
    @Mock
    GoogleSheetService googleSheetService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(googleConfig, "credentialsFilePath", "src/main/resources/key/google_spread_sheet_key.json");
        ReflectionTestUtils.setField(googleConfig, "spreadSheetId", "1PKgmtFVrJWw4briZGxlfyKKUd3XaQscsmAGR6LZ12Os");
    }

    @Test
    void getSheetValuesTest() throws Exception {
        //given
        Method method = googleConfig.getClass().getDeclaredMethod("getSheetValues");
        method.setAccessible(true);

        //when
        List<GoogleSheetDto> list = (List<GoogleSheetDto>) method.invoke(googleConfig);

        //then
        assertThat(list).isNotEmpty();
    }
}