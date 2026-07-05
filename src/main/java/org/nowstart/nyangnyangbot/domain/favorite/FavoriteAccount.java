package org.nowstart.nyangnyangbot.domain.favorite;

import lombok.Getter;

@Getter
public class FavoriteAccount {

    private final String userId;
    private String nickName;
    private int balance;

    private FavoriteAccount(String userId, String nickName, int balance) {
        if (isBlank(userId)) {
            throw new IllegalArgumentException("userId is required");
        }
        this.userId = userId;
        this.nickName = nickName == null ? "" : nickName;
        this.balance = balance;
    }

    public static FavoriteAccount of(String userId, String nickName, Integer balance) {
        return new FavoriteAccount(userId, nickName, balance == null ? 0 : balance);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public FavoriteBalanceChange applyDelta(int delta, boolean allowNegativeBalance) {
        if (delta == 0) {
            throw new IllegalArgumentException("delta must not be zero");
        }
        int before = balance;
        int after = before + delta;
        if (!allowNegativeBalance && after < 0) {
            throw new IllegalArgumentException("favorite balance cannot be negative");
        }
        balance = after;
        return new FavoriteBalanceChange(before, delta, after);
    }

    public void updateNickName(String nickName) {
        if (!isBlank(nickName)) {
            this.nickName = nickName;
        }
    }
}
