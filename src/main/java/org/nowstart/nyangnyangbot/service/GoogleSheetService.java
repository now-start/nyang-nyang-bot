package org.nowstart.nyangnyangbot.service;

import io.micrometer.common.util.StringUtils;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.data.dto.GoogleSheetDto;
import org.nowstart.nyangnyangbot.data.entity.FavoriteEntity;
import org.nowstart.nyangnyangbot.data.entity.FavoriteHistoryEntity;
import org.nowstart.nyangnyangbot.repository.FavoriteHistoryRepository;
import org.nowstart.nyangnyangbot.repository.FavoriteRepository;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class GoogleSheetService {

    private final DefaultGoogleSheetValuesProvider valuesProvider;
    private final FavoriteRepository favoriteRepository;
    private final FavoriteHistoryRepository favoriteHistoryRepository;

    public void updateFavorite() {
        List<GoogleSheetDto> googleSheetDtoList = getSheetValues(valuesProvider.fetchValues());

        for (GoogleSheetDto dto : googleSheetDtoList) {
            FavoriteEntity favoriteEntity = favoriteRepository.findById(dto.getUserId())
                .orElseGet(() -> favoriteRepository.save(FavoriteEntity.builder()
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
    }

    private List<GoogleSheetDto> getSheetValues(List<List<Object>> values) {
        return values.stream()
                .filter(value -> value.size() >= 3)
                .map(value -> {
                    log.info("[GoogleSheet] Row value: {}", value);
                    return GoogleSheetDto.builder()
                            .nickName((String) value.get(0))
                            .userId((String) value.get(1))
                            .favorite(Integer.parseInt(value.getLast().toString()))
                            .build();
                })
            .filter(dto -> !StringUtils.isBlank(dto.getUserId()))
            .collect(Collectors.toMap(
                GoogleSheetDto::getUserId,
                dto -> dto,
                (existing, replacement) -> replacement
            )).values().stream()
            .toList();
    }
}
