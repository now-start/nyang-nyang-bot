package org.nowstart.nyangnyangbot.application.service;

import io.micrometer.common.util.StringUtils;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.domain.model.FavoriteAdjustmentOption;
import org.nowstart.nyangnyangbot.application.favorite.AdjustFavoriteCommand;
import org.nowstart.nyangnyangbot.application.favorite.AdjustFavoriteUseCase;
import org.nowstart.nyangnyangbot.application.favorite.FavoriteLedgerResult;
import org.nowstart.nyangnyangbot.application.gateway.out.favorite.FavoriteAdjustmentPort;
import org.nowstart.nyangnyangbot.application.dto.favorite.FavoriteAdjustmentDto;
import org.nowstart.nyangnyangbot.domain.favorite.FavoriteSourceType;
import org.springframework.stereotype.Service;

@Service
@Transactional
@RequiredArgsConstructor
public class FavoriteAdjustmentService {

    private final FavoriteAdjustmentPort favoriteAdjustmentPort;
    private final AdjustFavoriteUseCase adjustFavoriteUseCase;

    public List<FavoriteAdjustmentOption> getAdjustments() {
        return favoriteAdjustmentPort.findAll()
                .stream()
                .sorted((left, right) -> Integer.compare(left.amount(), right.amount()))
                .toList();
    }

    public FavoriteAdjustmentOption createAdjustment(FavoriteAdjustmentDto.CreateRequest request) {
        validateCreateRequest(request);
        return favoriteAdjustmentPort.save(request.amount(), request.label().trim());
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

        List<FavoriteAdjustmentOption> adjustments = List.of();
        if (adjustmentIds != null && !adjustmentIds.isEmpty()) {
            adjustments = favoriteAdjustmentPort.findAllById(adjustmentIds);
            if (adjustments.size() != adjustmentIds.size()) {
                Map<Long, FavoriteAdjustmentOption> foundMap = adjustments.stream()
                        .collect(Collectors.toMap(FavoriteAdjustmentOption::id, entity -> entity));
                List<Long> missing = new ArrayList<>();
                for (Long id : adjustmentIds) {
                    if (!foundMap.containsKey(id)) {
                        missing.add(id);
                    }
                }
                throw new IllegalArgumentException("Missing adjustments: " + missing);
            }
        }

        int delta = adjustments.stream()
                .mapToInt(FavoriteAdjustmentOption::amount)
                .sum();
        if (hasManualAmount) {
            delta += manualAmount;
        }

        String history = buildHistory(adjustments, manualAmount, manualHistory);
        FavoriteLedgerResult result = adjustFavoriteUseCase.adjust(AdjustFavoriteCommand.builder()
                .userId(userId)
                .delta(delta)
                .sourceType(FavoriteSourceType.ADMIN_ADJUSTMENT)
                .displayCategory("ADMIN")
                .publicDescription(history)
                .allowNegativeBalance(true)
                .createIfMissing(false)
                .build());

        return new FavoriteAdjustmentDto.ApplyResponse(
                userId,
                result.beforeBalance(),
                result.delta(),
                result.afterBalance(),
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

    private String buildHistory(
            List<FavoriteAdjustmentOption> adjustments,
            Integer manualAmount,
            String manualHistory
    ) {
        List<String> parts = new ArrayList<>();
        for (FavoriteAdjustmentOption entity : adjustments) {
            parts.add(String.format(Locale.ROOT, "%s(%+d)", entity.label(), entity.amount()));
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
