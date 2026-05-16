package org.nowstart.nyangnyangbot.application.service.favorite;

import io.micrometer.common.util.StringUtils;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.application.port.in.favorite.AdjustFavoriteUseCase.AdjustFavoriteCommand;
import org.nowstart.nyangnyangbot.application.port.in.favorite.ManageFavoriteAdjustmentUseCase.FavoriteAdjustmentApplyCommand;
import org.nowstart.nyangnyangbot.application.port.in.favorite.ManageFavoriteAdjustmentUseCase.FavoriteAdjustmentApplyResult;
import org.nowstart.nyangnyangbot.application.port.in.favorite.ManageFavoriteAdjustmentUseCase.FavoriteAdjustmentCreateCommand;
import org.nowstart.nyangnyangbot.application.port.in.favorite.ManageFavoriteAdjustmentUseCase.FavoriteAdjustmentOptionResult;
import org.nowstart.nyangnyangbot.application.port.in.favorite.AdjustFavoriteUseCase.FavoriteLedgerResult;
import org.nowstart.nyangnyangbot.application.port.in.favorite.AdjustFavoriteUseCase;
import org.nowstart.nyangnyangbot.application.port.in.favorite.ManageFavoriteAdjustmentUseCase;
import org.nowstart.nyangnyangbot.application.port.out.favorite.FavoriteAdjustmentPort;
import org.nowstart.nyangnyangbot.domain.favorite.FavoriteSourceType;
import org.nowstart.nyangnyangbot.application.port.out.favorite.FavoriteAdjustmentPort.OptionResult;
import org.springframework.stereotype.Service;

@Service
@Transactional
@RequiredArgsConstructor
public class FavoriteAdjustmentService implements ManageFavoriteAdjustmentUseCase {

    private final FavoriteAdjustmentPort favoriteAdjustmentPort;
    private final AdjustFavoriteUseCase adjustFavoriteUseCase;

    @Override
    public List<FavoriteAdjustmentOptionResult> getAdjustments() {
        return favoriteAdjustmentPort.findAll()
                .stream()
                .sorted((left, right) -> Integer.compare(left.amount(), right.amount()))
                .map(this::favoriteAdjustmentOptionResult)
                .toList();
    }

    @Override
    public FavoriteAdjustmentOptionResult createAdjustment(FavoriteAdjustmentCreateCommand command) {
        validateCreateCommand(command);
        return favoriteAdjustmentOptionResult(favoriteAdjustmentPort.save(command.amount(), command.label().trim()));
    }

    @Override
    public FavoriteAdjustmentApplyResult applyAdjustments(FavoriteAdjustmentApplyCommand command) {
        String userId = command.userId();
        if (StringUtils.isBlank(userId)) {
            throw new IllegalArgumentException("userId is required");
        }
        List<Long> adjustmentIds = command.adjustmentIds();
        Integer manualAmount = command.manualAmount();
        String manualHistory = command.manualHistory();
        boolean hasManualAmount = manualAmount != null && manualAmount != 0;
        if ((adjustmentIds == null || adjustmentIds.isEmpty()) && !hasManualAmount) {
            throw new IllegalArgumentException("adjustmentIds or manualAmount is required");
        }

        List<OptionResult> adjustments = List.of();
        if (adjustmentIds != null && !adjustmentIds.isEmpty()) {
            adjustments = favoriteAdjustmentPort.findAllById(adjustmentIds);
            if (adjustments.size() != adjustmentIds.size()) {
                Map<Long, OptionResult> foundMap = adjustments.stream()
                        .collect(Collectors.toMap(OptionResult::id, entity -> entity));
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
                .mapToInt(OptionResult::amount)
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

        return new FavoriteAdjustmentApplyResult(
                userId,
                result.beforeBalance(),
                result.delta(),
                result.afterBalance(),
                history
        );
    }

    private void validateCreateCommand(FavoriteAdjustmentCreateCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("request is required");
        }
        if (command.amount() == null) {
            throw new IllegalArgumentException("amount is required");
        }
        if (StringUtils.isBlank(command.label())) {
            throw new IllegalArgumentException("label is required");
        }
    }

    private String buildHistory(
            List<OptionResult> adjustments,
            Integer manualAmount,
            String manualHistory
    ) {
        List<String> parts = new ArrayList<>();
        for (OptionResult entity : adjustments) {
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

    private FavoriteAdjustmentOptionResult favoriteAdjustmentOptionResult(OptionResult option) {
        return new FavoriteAdjustmentOptionResult(
                option.id(),
                option.amount(),
                option.label()
        );
    }
}
