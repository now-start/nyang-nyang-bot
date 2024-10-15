package org.nowstart.chzzk_favorite_bot.service;

import io.micrometer.common.util.StringUtils;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.nowstart.chzzk_favorite_bot.data.entity.FavoriteEntity;
import org.nowstart.chzzk_favorite_bot.data.entity.FavoriteHistoryEntity;
import org.nowstart.chzzk_favorite_bot.repository.FavoriteHistoryRepository;
import org.nowstart.chzzk_favorite_bot.repository.FavoriteRepository;
import org.springframework.stereotype.Service;

@Service
@Transactional
@RequiredArgsConstructor
public class GoogleSheetService {

    private final FavoriteRepository favoriteRepository;
    private final FavoriteHistoryRepository favoriteHistoryRepository;

    public void updateFavorite(List<List<Object>> rows) {
        List<FavoriteHistoryEntity> favoriteHistoryEntityList = new ArrayList<>();

        for (List<Object> row : rows) {
            String nickName = (String) row.get(0);
            String userId = (String) row.get(1);
            int sheetFavorite = Integer.parseInt((String) row.get(row.size() - 1));

            if (!StringUtils.isBlank(userId)) {
                Optional<FavoriteEntity> optionalFavoriteEntity = favoriteRepository.findByUserId(userId);
                FavoriteEntity favoriteEntity = favoriteRepository.findByUserId(userId).orElse(FavoriteEntity.builder()
                    .userId(userId)
                    .nickName(nickName)
                    .build());

                int addFavorite = sheetFavorite - favoriteEntity.getFavorite();
                if (optionalFavoriteEntity.isEmpty() || addFavorite != 0) {
                    favoriteHistoryEntityList.add(FavoriteHistoryEntity.builder()
                        .favoriteEntity(favoriteEntity.updateNickName(nickName).addFavorite(addFavorite))
                        .favorite(favoriteEntity.getFavorite())
                        .history("데이터 동기화")
                        .build());
                }
            }
        }

        favoriteHistoryRepository.saveAll(favoriteHistoryEntityList);
    }
}