package org.nowstart.nyangnyangbot.repository;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.nowstart.nyangnyangbot.data.dto.GoogleSheetDto;
import org.nowstart.nyangnyangbot.data.entity.FavoriteEntity;
import org.nowstart.nyangnyangbot.data.entity.FavoriteHistoryEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class FavoriteRepositoryTest {

    @Autowired
    FavoriteRepository favoriteRepository;
    @Autowired
    FavoriteHistoryRepository favoriteHistoryRepository;

    @Test
    void test1() {
        //given
        FavoriteEntity testUser1 = FavoriteEntity.builder()
                .userId("testUser1")
                .favorite(1)
                .build();
        favoriteRepository.saveAndFlush(testUser1);

        favoriteHistoryRepository.saveAndFlush(FavoriteHistoryEntity.builder()
                .history("테스트")
                .favoriteEntity(testUser1)
                .favorite(1)
                .build());


        List<GoogleSheetDto> googleSheetDtoList = List.of(
                GoogleSheetDto.builder()
                        .userId("testUser1")
                        .favorite(2)
                        .build(),
                GoogleSheetDto.builder()
                        .userId("testUser2")
                        .favorite(2)
                        .build());


        //when
        System.out.println("=================================================");
        for (GoogleSheetDto dto : googleSheetDtoList) {
            FavoriteEntity favoriteEntity = favoriteRepository.findById(dto.getUserId())
                    .orElseGet(() -> favoriteRepository.saveAndFlush(FavoriteEntity.builder()
                            .userId(dto.getUserId())
                            .nickName(dto.getNickName())
                            .favorite(0)
                            .build()));

            if (!favoriteEntity.getFavorite().equals(dto.getFavorite())) {
                favoriteEntity.setNickName(dto.getNickName());
                favoriteEntity.setFavorite(dto.getFavorite());
                favoriteEntity.getFavoriteHistoryEntityList().add(FavoriteHistoryEntity.builder()
                        .favoriteEntity(favoriteEntity)
                        .history("데이터 동기화")
                        .favorite(dto.getFavorite())
                        .build());
            }
        }


        //then
        System.out.println(favoriteRepository.findById("testUser1"));
        System.out.println(favoriteRepository.findById("testUser2"));

    }
}