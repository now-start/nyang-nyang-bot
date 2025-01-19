package org.nowstart.nyangnyangbot.service;

import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.data.dto.GoogleSheetDto;
import org.nowstart.nyangnyangbot.data.entity.FavoriteEntity;
import org.nowstart.nyangnyangbot.data.entity.FavoriteHistoryEntity;
import org.nowstart.nyangnyangbot.repository.FavoriteHistoryRepository;
import org.nowstart.nyangnyangbot.repository.FavoriteRepository;
import org.springframework.stereotype.Service;

@Service
@Transactional
@RequiredArgsConstructor
public class GoogleSheetService {

    private final FavoriteRepository favoriteRepository;
    private final FavoriteHistoryRepository favoriteHistoryRepository;

    public void updateFavorite(List<GoogleSheetDto> list) {
        List<FavoriteHistoryEntity> favoriteHistoryEntityList = new ArrayList<>();

        for (GoogleSheetDto googleSheetDto : list) {
            String nickName = googleSheetDto.getNickName();
            String userId = googleSheetDto.getUserId();
            int sheetFavorite = googleSheetDto.getFavorite();

            FavoriteEntity favoriteEntity = favoriteRepository.findByUserId(userId).orElse(FavoriteEntity.builder()
                .userId(userId)
                .nickName(nickName)
                .build());

            int addFavorite = sheetFavorite - favoriteEntity.getFavorite();
            if (addFavorite != 0) {
                favoriteHistoryEntityList.add(FavoriteHistoryEntity.builder()
                    .favoriteEntity(favoriteEntity.updateNickName(nickName).addFavorite(addFavorite))
                    .favorite(favoriteEntity.getFavorite())
                    .history("데이터 동기화")
                    .build());
            }
        }

        favoriteHistoryRepository.saveAll(favoriteHistoryEntityList);
    }
}