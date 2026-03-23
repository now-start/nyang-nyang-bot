package org.nowstart.nyangnyangbot.service;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.data.dto.favorite.FavoriteAdjustmentDto;
import org.nowstart.nyangnyangbot.data.entity.FavoriteAdjustmentEntity;
import org.nowstart.nyangnyangbot.data.entity.FavoriteEntity;
import org.nowstart.nyangnyangbot.data.entity.FavoriteHistoryEntity;
import org.nowstart.nyangnyangbot.data.entity.UserKarmaEntity;
import org.nowstart.nyangnyangbot.data.type.FavoriteHistoryType;
import org.nowstart.nyangnyangbot.data.type.FavoriteKarmaSourceType;
import org.nowstart.nyangnyangbot.repository.FavoriteAdjustmentRepository;
import org.nowstart.nyangnyangbot.repository.FavoriteHistoryRepository;
import org.nowstart.nyangnyangbot.repository.UserKarmaRepository;

@ExtendWith(MockitoExtension.class)
class FavoriteAdjustmentServiceTest {

    @Mock
    private FavoriteAdjustmentRepository favoriteAdjustmentRepository;

    @Mock
    private UserKarmaRepository userKarmaRepository;

    @Mock
    private FavoriteHistoryRepository favoriteHistoryRepository;

    @Mock
    private FavoriteAggregationService favoriteAggregationService;

    @InjectMocks
    private FavoriteAdjustmentService favoriteAdjustmentService;

    @Test
    void applyAdjustments_ShouldPersistKarmaEntriesAndHistory() {
        FavoriteEntity favoriteEntity = FavoriteEntity.builder()
                .userId("user1")
                .nickName("유저")
                .totalFavorite(20)
                .karmaScore(20)
                .attendanceCount(0)
                .build();
        List<FavoriteAdjustmentEntity> adjustments = List.of(
                FavoriteAdjustmentEntity.builder().id(1L).amount(7).label("칭찬").build(),
                FavoriteAdjustmentEntity.builder().id(2L).amount(-3).label("지각").build()
        );
        given(favoriteAdjustmentRepository.findAllById(List.of(1L, 2L))).willReturn(adjustments);
        given(favoriteAggregationService.getOrCreateFavorite("user1", "")).willReturn(favoriteEntity);
        given(favoriteAggregationService.recalculate(favoriteEntity))
                .willReturn(new FavoriteAggregationService.FavoriteTotals(24, 0, 24));

        FavoriteAdjustmentDto.ApplyResponse response = favoriteAdjustmentService.applyAdjustments(
                "user1",
                List.of(1L, 2L),
                null,
                null
        );

        then(response.userId()).isEqualTo("user1");
        then(response.beforeFavorite()).isEqualTo(20);
        then(response.delta()).isEqualTo(4);
        then(response.afterFavorite()).isEqualTo(24);
        then(response.history()).isEqualTo("업보 적용: 칭찬(+7), 지각(-3)");

        ArgumentCaptor<Iterable> captor = ArgumentCaptor.forClass(Iterable.class);
        BDDMockito.then(userKarmaRepository).should().saveAll(captor.capture());
        List<UserKarmaEntity> savedKarmas = toList(captor.getValue());
        then(savedKarmas).hasSize(2);
        then(savedKarmas).allMatch(karma -> FavoriteKarmaSourceType.ADJUSTMENT.equals(karma.getSourceType()));
        then(savedKarmas).allMatch(karma -> Integer.valueOf(1).equals(karma.getQuantity()));
        then(savedKarmas).extracting(UserKarmaEntity::getAmount).containsExactly(7, -3);
        BDDMockito.then(favoriteHistoryRepository).should().save(any(FavoriteHistoryEntity.class));
        BDDMockito.then(favoriteHistoryRepository).should().save(org.mockito.ArgumentMatchers.argThat(history ->
                history.getFavoriteEntity() == favoriteEntity
                        && Integer.valueOf(24).equals(history.getFavorite())
                        && Integer.valueOf(24).equals(history.getKarmaScore())
                        && Integer.valueOf(0).equals(history.getAttendanceCount())
                        && FavoriteHistoryType.KARMA.equals(history.getType())
                        && "업보 적용: 칭찬(+7), 지각(-3)".equals(history.getHistory())
        ));
    }

    @Test
    void createAdjustment_ShouldTrimLabel() {
        FavoriteAdjustmentEntity saved = FavoriteAdjustmentEntity.builder().id(3L).amount(5).label("보정").build();
        given(favoriteAdjustmentRepository.save(any(FavoriteAdjustmentEntity.class))).willReturn(saved);

        FavoriteAdjustmentEntity result = favoriteAdjustmentService.createAdjustment(
                new FavoriteAdjustmentDto.CreateRequest(5, "  보정  ")
        );

        then(result).isSameAs(saved);
        BDDMockito.then(favoriteAdjustmentRepository).should().save(org.mockito.ArgumentMatchers.argThat(entity ->
                Integer.valueOf(5).equals(entity.getAmount())
                        && "보정".equals(entity.getLabel())
        ));
    }

    private List<UserKarmaEntity> toList(Iterable<?> iterable) {
        return iterable == null ? List.of() : java.util.stream.StreamSupport.stream(iterable.spliterator(), false)
                .map(UserKarmaEntity.class::cast)
                .toList();
    }
}
