package org.nowstart.nyangnyangbot.domain.favorite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class FavoriteAccountTest {

    @Test
    void applyDelta_ShouldUpdateBalanceAndReturnBeforeAfter() {
        FavoriteAccount account = FavoriteAccount.of("user-1", "치즈냥", 10);

        FavoriteBalanceChange result = account.applyDelta(5, false);

        assertThat(result.beforeBalance()).isEqualTo(10);
        assertThat(result.delta()).isEqualTo(5);
        assertThat(result.afterBalance()).isEqualTo(15);
        assertThat(account.getBalance()).isEqualTo(15);
    }

    @Test
    void applyDelta_ShouldRejectNegativeBalance_WhenPolicyDisallowsIt() {
        FavoriteAccount account = FavoriteAccount.of("user-1", "치즈냥", 3);

        assertThatThrownBy(() -> account.applyDelta(-5, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("favorite balance cannot be negative");
        assertThat(account.getBalance()).isEqualTo(3);
    }

    @Test
    void applyDelta_ShouldAllowNegativeBalance_WhenPolicyAllowsIt() {
        FavoriteAccount account = FavoriteAccount.of("user-1", "치즈냥", 3);

        FavoriteBalanceChange result = account.applyDelta(-5, true);

        assertThat(result.afterBalance()).isEqualTo(-2);
        assertThat(account.getBalance()).isEqualTo(-2);
    }
}
