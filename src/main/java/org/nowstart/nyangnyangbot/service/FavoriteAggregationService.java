package org.nowstart.nyangnyangbot.service;

import io.micrometer.common.util.StringUtils;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.data.entity.FavoriteEntity;
import org.nowstart.nyangnyangbot.data.entity.UserKarmaEntity;
import org.nowstart.nyangnyangbot.data.type.FavoriteKarmaSourceType;
import org.nowstart.nyangnyangbot.repository.FavoriteRepository;
import org.nowstart.nyangnyangbot.repository.UserKarmaRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FavoriteAggregationService {

    private final FavoriteRepository favoriteRepository;
    private final UserKarmaRepository userKarmaRepository;

    public FavoriteEntity getOrCreateFavorite(String userId, String nickName) {
        return favoriteRepository.findById(userId)
                .orElseGet(() -> favoriteRepository.save(FavoriteEntity.builder()
                        .userId(userId)
                        .nickName(StringUtils.isBlank(nickName) ? "" : nickName)
                        .totalFavorite(0)
                        .karmaScore(0)
                        .attendanceCount(0)
                        .build()));
    }

    public FavoriteTotals recalculate(FavoriteEntity favoriteEntity) {
        List<UserKarmaEntity> karmas = userKarmaRepository.findAllByFavoriteEntityUserId(favoriteEntity.getUserId());
        int karmaScore = karmas.stream()
                .mapToInt(this::resolveKarmaAmount)
                .sum();
        int attendanceCount = favoriteEntity.safeAttendanceCount();
        int totalFavorite = karmaScore + attendanceCount;
        favoriteEntity.setKarmaScore(karmaScore);
        favoriteEntity.setTotalFavorite(totalFavorite);
        return new FavoriteTotals(karmaScore, attendanceCount, totalFavorite);
    }

    public int calculateNonSyncKarmaScore(String userId) {
        return userKarmaRepository.findAllByFavoriteEntityUserId(userId).stream()
                .filter(karma -> karma.getSourceType() != FavoriteKarmaSourceType.SYNC)
                .mapToInt(this::resolveKarmaAmount)
                .sum();
    }

    private int resolveKarmaAmount(UserKarmaEntity karma) {
        int quantity = Objects.requireNonNullElse(karma.getQuantity(), 0);
        int amount = Objects.requireNonNullElse(karma.getAmount(), 0);
        return quantity * amount;
    }

    public record FavoriteTotals(int karmaScore, int attendanceCount, int totalFavorite) {
    }
}
