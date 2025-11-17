package org.nowstart.nyangnyangbot.service;

import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import io.micrometer.common.util.StringUtils;
import jakarta.transaction.Transactional;
import java.io.FileInputStream;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.data.dto.GoogleSheetDto;
import org.nowstart.nyangnyangbot.data.entity.FavoriteEntity;
import org.nowstart.nyangnyangbot.data.entity.FavoriteHistoryEntity;
import org.nowstart.nyangnyangbot.data.property.GoogleProperty;
import org.nowstart.nyangnyangbot.repository.FavoriteHistoryRepository;
import org.nowstart.nyangnyangbot.repository.FavoriteRepository;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class GoogleSheetService {

    private static final String RANGE = "호감도 순위표!B2:H2000";
    private final GoogleProperty googleProperty;
    private final FavoriteRepository favoriteRepository;
    private final FavoriteHistoryRepository favoriteHistoryRepository;

    public void updateFavorite() {
        List<GoogleSheetDto> googleSheetDtoList = getSheetValues();

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

    @SneakyThrows
    private List<GoogleSheetDto> getSheetValues() {
        GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream(googleProperty.getKey())).createScoped(SheetsScopes.SPREADSHEETS);
        Sheets sheets = new Sheets.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance(), new HttpCredentialsAdapter(credentials))
            .setApplicationName("google-sheet-project")
            .build();

        List<List<Object>> values = sheets.spreadsheets()
            .values()
            .get(googleProperty.getId(), RANGE)
            .execute().getValues();

        return values.stream()
            .map(value -> GoogleSheetDto.builder()
                .nickName((String) value.get(0))
                .userId((String) value.get(1))
                .favorite(Integer.parseInt((String) value.get(value.size() - 1)))
                .build())
            .filter(dto -> !StringUtils.isBlank(dto.getUserId()))
            .collect(Collectors.toMap(
                GoogleSheetDto::getUserId,
                dto -> dto,
                (existing, replacement) -> replacement
            )).values().stream()
            .toList();
    }
}