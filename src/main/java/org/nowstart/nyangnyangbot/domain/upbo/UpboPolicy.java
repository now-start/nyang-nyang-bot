package org.nowstart.nyangnyangbot.domain.upbo;

import org.nowstart.nyangnyangbot.domain.type.ConversionMode;
import org.nowstart.nyangnyangbot.domain.type.RewardType;
import org.nowstart.nyangnyangbot.domain.type.UpboStatus;

public class UpboPolicy {

    public void validateTemplate(
            String label,
            RewardType rewardType,
            ConversionMode conversionMode,
            Integer exchangeFavoriteValue
    ) {
        if (isBlank(label)) {
            throw new IllegalArgumentException("label is required");
        }
        validateReward(rewardType, conversionMode);
        validateAutoExchange(conversionMode, exchangeFavoriteValue);
    }

    public void validateApply(
            String userId,
            boolean templateSelected,
            String label,
            RewardType rewardType,
            ConversionMode conversionMode,
            Integer exchangeFavoriteValue,
            String publicDescription,
            String privateMemo
    ) {
        if (isBlank(userId)) {
            throw new IllegalArgumentException("userId is required");
        }
        if (!templateSelected) {
            if (isBlank(label)) {
                throw new IllegalArgumentException("label is required");
            }
            validateReward(rewardType, conversionMode);
        }
        validateAutoExchange(conversionMode, exchangeFavoriteValue);
        if (isBlank(publicDescription)) {
            throw new IllegalArgumentException("publicDescription is required");
        }
        if (isBlank(privateMemo)) {
            throw new IllegalArgumentException("privateMemo is required");
        }
    }

    public boolean requiresFavoriteAdjustment(ConversionMode conversionMode) {
        return conversionMode == ConversionMode.AUTO;
    }

    public UpboStatus initialStatus(ConversionMode conversionMode) {
        return requiresFavoriteAdjustment(conversionMode)
                ? UpboStatus.CONVERTED
                : UpboStatus.OWNED;
    }

    private void validateReward(RewardType rewardType, ConversionMode conversionMode) {
        if (rewardType == null) {
            throw new IllegalArgumentException("rewardType is required");
        }
        if (conversionMode == null) {
            throw new IllegalArgumentException("conversionMode is required");
        }
    }

    private void validateAutoExchange(ConversionMode conversionMode, Integer exchangeFavoriteValue) {
        if (conversionMode == ConversionMode.AUTO
                && (exchangeFavoriteValue == null || exchangeFavoriteValue == 0)) {
            throw new IllegalArgumentException("exchangeFavoriteValue is required for AUTO conversion");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
