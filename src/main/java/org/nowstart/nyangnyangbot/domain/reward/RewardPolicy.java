package org.nowstart.nyangnyangbot.domain.reward;

import org.nowstart.nyangnyangbot.domain.type.ConversionMode;
import org.nowstart.nyangnyangbot.domain.type.RewardGrantStatus;
import org.nowstart.nyangnyangbot.domain.type.RewardType;

public class RewardPolicy {

    public void validateGrant(
            String userId,
            Long rouletteRoundId,
            Long pointLedgerEntryId,
            String label,
            RewardType rewardType,
            ConversionMode conversionMode,
            Long pointDelta,
            RewardGrantStatus status,
            String description,
            String actorUserId,
            String idempotencyKey
    ) {
        requireText(userId, "userId");
        requireText(label, "label");
        requireText(description, "description");
        requireText(idempotencyKey, "idempotencyKey");
        if (rewardType == null) {
            throw new IllegalArgumentException("rewardType is required");
        }
        if (conversionMode == null) {
            throw new IllegalArgumentException("conversionMode is required");
        }
        if (status == null) {
            throw new IllegalArgumentException("status is required");
        }
        boolean rouletteOrigin = rouletteRoundId != null;
        boolean manualOrigin = !isBlank(actorUserId);
        if (rouletteOrigin == manualOrigin) {
            throw new IllegalArgumentException("exactly one reward origin is required");
        }
        validateConversion(conversionMode, pointDelta, pointLedgerEntryId, status);
    }

    public RewardGrantStatus initialStatus(ConversionMode conversionMode) {
        return conversionMode == ConversionMode.AUTO
                ? RewardGrantStatus.CONVERTED
                : RewardGrantStatus.OWNED;
    }

    private void validateConversion(
            ConversionMode mode,
            Long pointDelta,
            Long pointLedgerEntryId,
            RewardGrantStatus status
    ) {
        if (mode == ConversionMode.AUTO) {
            if (pointDelta == null || pointDelta == 0 || pointLedgerEntryId == null) {
                throw new IllegalArgumentException("AUTO reward requires point delta and ledger entry");
            }
            if (status != RewardGrantStatus.CONVERTED && status != RewardGrantStatus.CORRECTED) {
                throw new IllegalArgumentException("AUTO reward status must be CONVERTED or CORRECTED");
            }
            return;
        }
        if (pointLedgerEntryId != null) {
            throw new IllegalArgumentException("non-AUTO reward must not reference a ledger entry");
        }
        if (mode == ConversionMode.NONE && pointDelta != null) {
            throw new IllegalArgumentException("NONE reward must not have point delta");
        }
        if (status != RewardGrantStatus.OWNED && status != RewardGrantStatus.USED) {
            throw new IllegalArgumentException("non-AUTO reward status must be OWNED or USED");
        }
    }

    private void requireText(String value, String field) {
        if (isBlank(value)) {
            throw new IllegalArgumentException(field + " is required");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
