package org.nowstart.nyangnyangbot.domain.upbo;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenCode;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;

import org.junit.jupiter.api.Test;
import org.nowstart.nyangnyangbot.domain.type.ConversionMode;
import org.nowstart.nyangnyangbot.domain.type.RewardType;
import org.nowstart.nyangnyangbot.domain.type.UpboStatus;

class UpboPolicyTest {

    private final UpboPolicy upboPolicy = new UpboPolicy();

    @Test
    void validateTemplate_ShouldAcceptValidTemplateAndRejectMissingLabel() {
        // 실행 및 검증
        thenCode(() -> upboPolicy.validateTemplate(
                "칭찬 쿠폰",
                RewardType.COUPON,
                ConversionMode.NONE,
                null
        )).doesNotThrowAnyException();
        thenThrownBy(() -> upboPolicy.validateTemplate(
                " ",
                RewardType.COUPON,
                ConversionMode.NONE,
                null
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("label is required");
    }

    @Test
    void validateTemplate_ShouldRejectMissingRewardAndConversion() {
        // 실행 및 검증
        thenThrownBy(() -> upboPolicy.validateTemplate(
                "업보",
                null,
                ConversionMode.NONE,
                null
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("rewardType is required");
        thenThrownBy(() -> upboPolicy.validateTemplate(
                "업보",
                RewardType.CUSTOM,
                null,
                null
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("conversionMode is required");
        thenThrownBy(() -> upboPolicy.validateTemplate(
                "업보",
                RewardType.FAVORITE,
                ConversionMode.AUTO,
                0
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("exchangeFavoriteValue is required for AUTO conversion");
    }

    @Test
    void validateApply_ShouldAllowTemplateSelectedWithoutFreeInputRewardFields() {
        // 실행 및 검증
        thenCode(() -> upboPolicy.validateApply(
                "user-1",
                true,
                null,
                null,
                ConversionMode.NONE,
                null,
                "공개 설명",
                "내부 메모"
        )).doesNotThrowAnyException();
    }

    @Test
    void validateApply_ShouldRejectRequiredFreeInputFields() {
        // 실행 및 검증
        thenThrownBy(() -> upboPolicy.validateApply(
                null,
                false,
                "업보",
                RewardType.CUSTOM,
                ConversionMode.NONE,
                null,
                "공개 설명",
                "내부 메모"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("userId is required");
        thenThrownBy(() -> upboPolicy.validateApply(
                "user-1",
                false,
                " ",
                RewardType.CUSTOM,
                ConversionMode.NONE,
                null,
                "공개 설명",
                "내부 메모"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("label is required");
        thenThrownBy(() -> upboPolicy.validateApply(
                "user-1",
                false,
                "업보",
                null,
                ConversionMode.NONE,
                null,
                "공개 설명",
                "내부 메모"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("rewardType is required");
        thenThrownBy(() -> upboPolicy.validateApply(
                "user-1",
                false,
                "업보",
                RewardType.CUSTOM,
                null,
                null,
                "공개 설명",
                "내부 메모"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("conversionMode is required");
    }

    @Test
    void validateApply_ShouldRejectAutoExchangeAndDescriptions() {
        // 실행 및 검증
        thenThrownBy(() -> upboPolicy.validateApply(
                "user-1",
                false,
                "업보",
                RewardType.FAVORITE,
                ConversionMode.AUTO,
                null,
                "공개 설명",
                "내부 메모"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("exchangeFavoriteValue is required for AUTO conversion");
        thenThrownBy(() -> upboPolicy.validateApply(
                "user-1",
                false,
                "업보",
                RewardType.CUSTOM,
                ConversionMode.NONE,
                null,
                " ",
                "내부 메모"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("publicDescription is required");
        thenThrownBy(() -> upboPolicy.validateApply(
                "user-1",
                false,
                "업보",
                RewardType.CUSTOM,
                ConversionMode.NONE,
                null,
                "공개 설명",
                " "
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("privateMemo is required");
    }

    @Test
    void initialStatus_ShouldDependOnFavoriteAdjustmentRequirement() {
        // 실행 및 검증
        then(upboPolicy.requiresFavoriteAdjustment(ConversionMode.AUTO)).isTrue();
        then(upboPolicy.requiresFavoriteAdjustment(ConversionMode.NONE)).isFalse();
        then(upboPolicy.requiresFavoriteAdjustment(null)).isFalse();
        then(upboPolicy.initialStatus(ConversionMode.AUTO)).isEqualTo(UpboStatus.CONVERTED);
        then(upboPolicy.initialStatus(ConversionMode.NONE)).isEqualTo(UpboStatus.OWNED);
    }
}
