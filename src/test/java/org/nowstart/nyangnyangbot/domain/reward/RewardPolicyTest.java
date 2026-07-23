package org.nowstart.nyangnyangbot.domain.reward;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.nowstart.nyangnyangbot.domain.type.ConversionMode;
import org.nowstart.nyangnyangbot.domain.type.RewardGrantStatus;
import org.nowstart.nyangnyangbot.domain.type.RewardType;

class RewardPolicyTest {

    private final RewardPolicy policy = new RewardPolicy();

    @Test
    void rouletteAutoRewardRequiresConvertedLedgerForSameGrant() {
        policy.validateGrant(
                "user-1",
                10L,
                20L,
                "포인트",
                RewardType.POINT,
                ConversionMode.AUTO,
                100L,
                RewardGrantStatus.CONVERTED,
                "룰렛 결과",
                null,
                "roulette-round:10"
        );
    }

    @Test
    void exactlyOneOriginIsRequired() {
        assertThatThrownBy(() -> policy.validateGrant(
                "user-1",
                10L,
                null,
                "쿠폰",
                RewardType.COUPON,
                ConversionMode.NONE,
                null,
                RewardGrantStatus.OWNED,
                "룰렛 결과",
                "admin-1",
                "reward-1"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("exactly one reward origin is required");
    }

    @Test
    void noneConversionCannotCarryPointDelta() {
        assertThatThrownBy(() -> policy.validateGrant(
                "user-1",
                null,
                null,
                "쿠폰",
                RewardType.COUPON,
                ConversionMode.NONE,
                10L,
                RewardGrantStatus.OWNED,
                "수동 지급",
                "admin-1",
                "reward-1"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("NONE reward must not have point delta");
    }

    @Test
    void initialStatusFollowsConversionMode() {
        assertThat(policy.initialStatus(ConversionMode.AUTO)).isEqualTo(RewardGrantStatus.CONVERTED);
        assertThat(policy.initialStatus(ConversionMode.MANUAL)).isEqualTo(RewardGrantStatus.OWNED);
    }
}
