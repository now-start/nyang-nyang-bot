package org.nowstart.nyangnyangbot.service;

import io.micrometer.common.util.StringUtils;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
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
import org.springframework.stereotype.Service;

@Service
@Transactional
@RequiredArgsConstructor
public class FavoriteAdjustmentService {

    private final FavoriteAdjustmentRepository favoriteAdjustmentRepository;
    private final UserKarmaRepository userKarmaRepository;
    private final FavoriteHistoryRepository favoriteHistoryRepository;
    private final FavoriteAggregationService favoriteAggregationService;

    public List<FavoriteAdjustmentEntity> getAdjustments() {
        return favoriteAdjustmentRepository.findAll()
                .stream()
                .sorted((left, right) -> Integer.compare(left.getAmount(), right.getAmount()))
                .toList();
    }

    public FavoriteAdjustmentEntity createAdjustment(FavoriteAdjustmentDto.CreateRequest request) {
        validateCreateRequest(request);
        return favoriteAdjustmentRepository.save(FavoriteAdjustmentEntity.builder()
                .amount(request.amount())
                .label(request.label().trim())
                .build());
    }

    public FavoriteAdjustmentDto.ApplyResponse applyAdjustments(
            String userId,
            List<Long> adjustmentIds,
            Integer manualAmount,
            String manualHistory
    ) {
        if (StringUtils.isBlank(userId)) {
            throw new IllegalArgumentException("userId is required");
        }
        boolean hasManualAmount = manualAmount != null && manualAmount != 0;
        if ((adjustmentIds == null || adjustmentIds.isEmpty()) && !hasManualAmount) {
            throw new IllegalArgumentException("adjustmentIds or manualAmount is required");
        }

        List<FavoriteAdjustmentEntity> adjustments = List.of();
        if (adjustmentIds != null && !adjustmentIds.isEmpty()) {
            adjustments = favoriteAdjustmentRepository.findAllById(adjustmentIds);
            if (adjustments.size() != adjustmentIds.size()) {
                Map<Long, FavoriteAdjustmentEntity> foundMap = adjustments.stream()
                        .collect(Collectors.toMap(FavoriteAdjustmentEntity::getId, entity -> entity));
                List<Long> missing = new ArrayList<>();
                for (Long id : adjustmentIds) {
                    if (!foundMap.containsKey(id)) {
                        missing.add(id);
                    }
                }
                throw new IllegalArgumentException("Missing adjustments: " + missing);
            }
        }

        FavoriteEntity favoriteEntity = favoriteAggregationService.getOrCreateFavorite(userId, "");
        int before = favoriteEntity.safeFavorite();
        userKarmaRepository.saveAll(buildKarmas(favoriteEntity, adjustments, manualAmount, manualHistory));
        FavoriteAggregationService.FavoriteTotals totals = favoriteAggregationService.recalculate(favoriteEntity);
        int after = totals.totalFavorite();
        int delta = after - before;

        String history = buildHistory(adjustments, manualAmount, manualHistory);
        favoriteHistoryRepository.save(FavoriteHistoryEntity.builder()
                .favoriteEntity(favoriteEntity)
                .history(history)
                .favorite(after)
                .karmaScore(totals.karmaScore())
                .attendanceCount(totals.attendanceCount())
                .type(FavoriteHistoryType.KARMA)
                .build());

        return new FavoriteAdjustmentDto.ApplyResponse(
                userId,
                before,
                delta,
                after,
                history
        );
    }

    private void validateCreateRequest(FavoriteAdjustmentDto.CreateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }
        if (request.amount() == null) {
            throw new IllegalArgumentException("amount is required");
        }
        if (StringUtils.isBlank(request.label())) {
            throw new IllegalArgumentException("label is required");
        }
    }

    private List<UserKarmaEntity> buildKarmas(
            FavoriteEntity favoriteEntity,
            List<FavoriteAdjustmentEntity> adjustments,
            Integer manualAmount,
            String manualHistory
    ) {
        List<UserKarmaEntity> karmas = new ArrayList<>();
        for (FavoriteAdjustmentEntity adjustment : adjustments) {
            karmas.add(UserKarmaEntity.builder()
                    .favoriteEntity(favoriteEntity)
                    .adjustment(adjustment)
                    .quantity(1)
                    .label(adjustment.getLabel())
                    .amount(adjustment.getAmount())
                    .sourceType(FavoriteKarmaSourceType.ADJUSTMENT)
                    .build());
        }
        if (manualAmount != null && manualAmount != 0) {
            karmas.add(UserKarmaEntity.builder()
                    .favoriteEntity(favoriteEntity)
                    .quantity(1)
                    .label(StringUtils.isBlank(manualHistory) ? "수동 입력" : manualHistory.trim())
                    .amount(manualAmount)
                    .sourceType(FavoriteKarmaSourceType.MANUAL)
                    .build());
        }
        return karmas;
    }

    private String buildHistory(
            List<FavoriteAdjustmentEntity> adjustments,
            Integer manualAmount,
            String manualHistory
    ) {
        List<String> parts = new ArrayList<>();
        for (FavoriteAdjustmentEntity entity : adjustments) {
            parts.add(String.format(Locale.ROOT, "%s(%+d)", entity.getLabel(), entity.getAmount()));
        }
        if (manualAmount != null && manualAmount != 0) {
            String label = StringUtils.isBlank(manualHistory) ? "수동 입력" : manualHistory.trim();
            parts.add(String.format(Locale.ROOT, "%s(%+d)", label, manualAmount));
        }
        if (parts.isEmpty()) {
            return "업보 적용";
        }
        return "업보 적용: " + String.join(", ", parts);
    }
}
