package org.nowstart.nyangnyangbot.domain.favorite;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FavoriteAccountTest {

    @Test
    @DisplayName("델타를 적용하면 잔액이 변경되고 변경 전후 값을 반환한다")
    void applyDelta_ShouldUpdateBalanceAndReturnBeforeAfter() {
        // 준비
        FavoriteAccount account = FavoriteAccount.of("user-1", "치즈냥", 10);

        // 실행
        FavoriteBalanceChange result = account.applyDelta(5, false);

        // 검증
        then(result.beforeBalance()).isEqualTo(10);
        then(result.delta()).isEqualTo(5);
        then(result.afterBalance()).isEqualTo(15);
        then(account.getBalance()).isEqualTo(15);
    }

    @Test
    @DisplayName("음수 잔액을 허용하지 않는 정책일 때 잔액이 마이너스가 되면 예외를 던진다")
    void applyDelta_ShouldRejectNegativeBalance_WhenPolicyDisallowsIt() {
        // 준비
        FavoriteAccount account = FavoriteAccount.of("user-1", "치즈냥", 3);

        // 실행 및 검증
        thenThrownBy(() -> account.applyDelta(-5, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("favorite balance cannot be negative");
        then(account.getBalance()).isEqualTo(3);
    }

    @Test
    @DisplayName("음수 잔액을 허용하는 정책일 때 잔액이 마이너스가 되어도 적용된다")
    void applyDelta_ShouldAllowNegativeBalance_WhenPolicyAllowsIt() {
        // 준비
        FavoriteAccount account = FavoriteAccount.of("user-1", "치즈냥", 3);

        // 실행
        FavoriteBalanceChange result = account.applyDelta(-5, true);

        // 검증
        then(result.afterBalance()).isEqualTo(-2);
        then(account.getBalance()).isEqualTo(-2);
    }
}
