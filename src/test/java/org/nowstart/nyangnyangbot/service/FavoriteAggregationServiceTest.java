package org.nowstart.nyangnyangbot.service;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.data.entity.FavoriteEntity;
import org.nowstart.nyangnyangbot.data.entity.UserKarmaEntity;
import org.nowstart.nyangnyangbot.data.type.FavoriteKarmaSourceType;
import org.nowstart.nyangnyangbot.repository.FavoriteRepository;
import org.nowstart.nyangnyangbot.repository.UserKarmaRepository;

@ExtendWith(MockitoExtension.class)
class FavoriteAggregationServiceTest {

    @Mock
    private FavoriteRepository favoriteRepository;

    @Mock
    private UserKarmaRepository userKarmaRepository;

    @InjectMocks
    private FavoriteAggregationService favoriteAggregationService;

    @Test
    void getOrCreateFavorite_ShouldReturnExistingEntity_WhenPresent() {
        FavoriteEntity favoriteEntity = FavoriteEntity.builder()
                .userId("user1")
                .nickName("기존")
                .totalFavorite(11)
                .karmaScore(7)
                .attendanceCount(4)
                .build();
        given(favoriteRepository.findById("user1")).willReturn(Optional.of(favoriteEntity));

        FavoriteEntity result = favoriteAggregationService.getOrCreateFavorite("user1", "새닉네임");

        then(result).isSameAs(favoriteEntity);
        BDDMockito.then(favoriteRepository).should().findById("user1");
    }

    @Test
    void getOrCreateFavorite_ShouldCreateZeroedEntity_WhenMissing() {
        given(favoriteRepository.findById("user2")).willReturn(Optional.empty());
        given(favoriteRepository.save(any(FavoriteEntity.class))).willAnswer(invocation -> invocation.getArgument(0));

        FavoriteEntity result = favoriteAggregationService.getOrCreateFavorite("user2", " ");

        then(result.getUserId()).isEqualTo("user2");
        then(result.getNickName()).isEqualTo("");
        then(result.getTotalFavorite()).isZero();
        then(result.safeFavorite()).isZero();
        then(result.safeKarmaScore()).isZero();
        then(result.safeAttendanceCount()).isZero();
    }

    @Test
    void recalculate_ShouldSyncKarmaScoreAndTotalFavorite() {
        FavoriteEntity favoriteEntity = FavoriteEntity.builder()
                .userId("user1")
                .attendanceCount(5)
                .build();
        given(userKarmaRepository.findAllByFavoriteEntityUserId("user1")).willReturn(List.of(
                userKarma(favoriteEntity, 1, 10, FavoriteKarmaSourceType.ADJUSTMENT),
                userKarma(favoriteEntity, 1, 5, FavoriteKarmaSourceType.MANUAL),
                userKarma(favoriteEntity, 1, null, FavoriteKarmaSourceType.SYNC)
        ));

        FavoriteAggregationService.FavoriteTotals totals = favoriteAggregationService.recalculate(favoriteEntity);

        then(totals.karmaScore()).isEqualTo(15);
        then(totals.attendanceCount()).isEqualTo(5);
        then(totals.totalFavorite()).isEqualTo(20);
        then(favoriteEntity.getKarmaScore()).isEqualTo(15);
        then(favoriteEntity.getFavorite()).isEqualTo(20);
        then(favoriteEntity.getTotalFavorite()).isEqualTo(20);
    }

    @Test
    void calculateNonSyncKarmaScore_ShouldIgnoreSyncEntries() {
        given(userKarmaRepository.findAllByFavoriteEntityUserId("user1")).willReturn(List.of(
                userKarma("user1", 1, 10, FavoriteKarmaSourceType.ADJUSTMENT),
                userKarma("user1", 1, 5, FavoriteKarmaSourceType.MANUAL),
                userKarma("user1", 1, 100, FavoriteKarmaSourceType.SYNC)
        ));

        int result = favoriteAggregationService.calculateNonSyncKarmaScore("user1");

        then(result).isEqualTo(15);
    }

    private UserKarmaEntity userKarma(
            FavoriteEntity favoriteEntity,
            Integer quantity,
            Integer amount,
            FavoriteKarmaSourceType sourceType
    ) {
        return UserKarmaEntity.builder()
                .favoriteEntity(favoriteEntity)
                .quantity(quantity)
                .amount(amount)
                .sourceType(sourceType)
                .label(sourceType.name())
                .build();
    }

    private UserKarmaEntity userKarma(
            String userId,
            Integer quantity,
            Integer amount,
            FavoriteKarmaSourceType sourceType
    ) {
        FavoriteEntity favoriteEntity = FavoriteEntity.builder().userId(userId).build();
        return userKarma(favoriteEntity, quantity, amount, sourceType);
    }
}
