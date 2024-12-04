package org.nowstart.nyangnyangbot.config;

import io.micrometer.common.util.StringUtils;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
class GoogleConfigTest {
    @InjectMocks
    GoogleConfig googleConfig;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(googleConfig, "credentialsFilePath", "src/main/resources/key/google_spread_sheet_key.json");
        ReflectionTestUtils.setField(googleConfig, "spreadSheetId", "1PKgmtFVrJWw4briZGxlfyKKUd3XaQscsmAGR6LZ12Os");
    }

    @Test
    void getSheetValuesTest() throws Exception {
        //given
        Method privateMethod = googleConfig.getClass().getDeclaredMethod("getSheetValues");
        privateMethod.setAccessible(true);

        //when
        List<List<Object>> rows = (List<List<Object>>) privateMethod.invoke(googleConfig);

        //then
        for (List<Object> row : rows) {
            String nickName = (String) row.get(0);
            String userId = (String) row.get(1);
            int totalFavorite = Integer.parseInt((String) row.get(row.size()-1));

            if(!StringUtils.isBlank(userId)){
                System.out.println("userId = " + userId);
                System.out.println("nickName = " + nickName);
                System.out.println("totalFavorite = " + totalFavorite);
            }
        }

        assertThat(rows).isNotEmpty();
    }
}