package org.nowstart.chzzk_like_bot.service;

import io.micrometer.common.util.StringUtils;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.nowstart.chzzk_like_bot.data.entity.FavoriteEntity;
import org.nowstart.chzzk_like_bot.data.entity.FavoriteHistoryEntity;
import org.nowstart.chzzk_like_bot.repository.FavoriteHistoryRepository;
import org.nowstart.chzzk_like_bot.repository.FavoriteRepository;
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
                FavoriteEntity favoriteEntity = optionalFavoriteEntity.orElse(FavoriteEntity.builder()
                    .userId(userId)
                    .nickName(nickName)
                    .build());

                int dbFavorite = favoriteEntity.getFavorite();
                if (optionalFavoriteEntity.isEmpty() || sheetFavorite != dbFavorite) {
                    favoriteHistoryEntityList.add(FavoriteHistoryEntity.builder()
                        .favoriteEntity(favoriteRepository.save(favoriteEntity.addFavorite(sheetFavorite - dbFavorite)))
                        .favorite(sheetFavorite - dbFavorite)
                        .history("데이터 동기화")
                        .build());
                }
            }
        }

        favoriteHistoryRepository.saveAll(favoriteHistoryEntityList);
    }
}