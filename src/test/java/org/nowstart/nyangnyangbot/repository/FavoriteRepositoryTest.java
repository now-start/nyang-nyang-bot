package org.nowstart.nyangnyangbot.repository;

import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.nowstart.nyangnyangbot.controller.GoogleController;
import org.nowstart.nyangnyangbot.data.dto.GoogleSheetDto;
import org.nowstart.nyangnyangbot.data.entity.FavoriteEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

@Disabled
@DataJpaTest
@ExtendWith(SpringExtension.class)
class FavoriteRepositoryTest {

    @Autowired
    FavoriteRepository favoriteRepository;
    @InjectMocks
    GoogleController googleController;

    @BeforeEach
    void setUp() throws Exception {
        ReflectionTestUtils.setField(googleController, "credentialsFilePath", "src/main/resources/key/google_spread_sheet_key.json");
        ReflectionTestUtils.setField(googleController, "spreadSheetId", "1PKgmtFVrJWw4briZGxlfyKKUd3XaQscsmAGR6LZ12Os");
        Method method = googleController.getClass().getDeclaredMethod("getSheetValues");
        method.setAccessible(true);
        List<GoogleSheetDto> list = (List<GoogleSheetDto>) method.invoke(googleController);

        list.forEach(googleSheetDto -> {
            FavoriteEntity favoriteEntity = FavoriteEntity.builder()
                .nickName(googleSheetDto.getNickName())
                .userId(googleSheetDto.getUserId())
                .favorite(googleSheetDto.getFavorite())
                .build();
            favoriteRepository.save(favoriteEntity);
        });
    }
}