package org.nowstart.nyangnyangbot.repository;

import java.lang.reflect.Method;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.nowstart.nyangnyangbot.config.GoogleConfig;
import org.nowstart.nyangnyangbot.data.dto.GoogleSheetDto;
import org.nowstart.nyangnyangbot.data.entity.FavoriteEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

@DataJpaTest
@ExtendWith(SpringExtension.class)
class FavoriteRepositoryTest {

    @Autowired
    FavoriteRepository favoriteRepository;
    @InjectMocks
    GoogleConfig googleConfig;

    @BeforeEach
    void setUp() throws Exception {
        ReflectionTestUtils.setField(googleConfig, "credentialsFilePath", "src/main/resources/key/google_spread_sheet_key.json");
        ReflectionTestUtils.setField(googleConfig, "spreadSheetId", "1PKgmtFVrJWw4briZGxlfyKKUd3XaQscsmAGR6LZ12Os");
        Method method = googleConfig.getClass().getDeclaredMethod("getSheetValues");
        method.setAccessible(true);
        List<GoogleSheetDto> list = (List<GoogleSheetDto>) method.invoke(googleConfig);

        list.forEach(googleSheetDto -> {
            FavoriteEntity favoriteEntity = FavoriteEntity.builder()
                .nickName(googleSheetDto.getNickName())
                .userId(googleSheetDto.getUserId())
                .favorite(googleSheetDto.getFavorite())
                .build();
            favoriteRepository.save(favoriteEntity);
        });
    }

    @ParameterizedTest
    @CsvSource({
        "1, 1, ",
        "2, 2, NULL",
        "3, 3, ",
        "4, 4, ",
        "5, 5, "
    })
    void findByNickNameContains(int index, int size, String nickName) {
        //given
        Pageable pageable = PageRequest.of(0, 5);

        //when
        Page<FavoriteEntity> result = favoriteRepository.findByNickNameContains(pageable, "테스트1");
        for (FavoriteEntity favoriteEntity : result) {
            System.out.println("favoriteEntity.getNickName() = " + favoriteEntity.getNickName());
        }

        //then
        Assertions.assertThat(result.getSize()).isEqualTo(5);
    }

    @Test
    void findByUserId() {
    }

    @Test
    void findByNickName() {
    }
}