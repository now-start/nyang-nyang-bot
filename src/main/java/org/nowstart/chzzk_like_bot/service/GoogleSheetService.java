package org.nowstart.chzzk_like_bot.service;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;
import io.micrometer.common.util.StringUtils;
import jakarta.transaction.Transactional;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.nowstart.chzzk_like_bot.entity.FavoriteEntity;
import org.nowstart.chzzk_like_bot.entity.FavoriteHistoryEntity;
import org.nowstart.chzzk_like_bot.repository.FavoriteHistoryRepository;
import org.nowstart.chzzk_like_bot.repository.FavoriteRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Transactional
@RequiredArgsConstructor
public class GoogleSheetService {

    @Value("${google.spreadsheet.id}")
    private String spreadSheetId;
    private final Sheets sheetsService;
    private final FavoriteRepository favoriteRepository;
    private final FavoriteHistoryRepository favoriteHistoryRepository;


    public List<List<Object>> getSheetValues(String range) throws IOException {
        ValueRange response = sheetsService.spreadsheets().values().get(spreadSheetId, range).execute();
        return response.getValues();
    }

    public void updateFavorite(List<List<Object>> rows) {
        for (List<Object> row : rows) {
            String nickName = (String) row.get(0);
            String userId = (String) row.get(1);
            int totalFavorite = Integer.parseInt((String) row.get(row.size()-1));

            if(!StringUtils.isBlank(userId)){
                updateFavorite(userId, nickName, totalFavorite);
            }
        }
    }

    private void updateFavorite(String userId, String nickName, int totalFavorite) {
        FavoriteEntity favoriteEntity = favoriteRepository.findByUserId(userId).orElse(
            FavoriteEntity.builder()
                .userId(userId)
                .nickName(nickName)
                .build());
        int addFavorite = totalFavorite - favoriteEntity.getFavorite();
        favoriteEntity.addFavorite(addFavorite);
        favoriteRepository.save(favoriteEntity);
        favoriteHistoryRepository.save(FavoriteHistoryEntity.builder()
            .favoriteEntity(favoriteEntity)
            .favorite(addFavorite)
            .history("데이터 동기화")
            .build());
    }
}