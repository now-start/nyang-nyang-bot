package org.nowstart.nyangnyangbot.service;

import io.micrometer.common.util.StringUtils;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
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

    public void updateFavorite(List<List<Object>> rows) {
        List<FavoriteHistoryEntity> favoriteHistoryEntityList = new ArrayList<>();

        for (List<Object> row : rows) {
            String nickName = (String) row.get(0);
            String userId = (String) row.get(1);
            int sheetFavorite = Integer.parseInt((String) row.get(row.size() - 1));

            if (!StringUtils.isBlank(userId)) {
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
        }

        favoriteHistoryRepository.saveAll(favoriteHistoryEntityList);
    }
}