package org.nowstart.nyangnyangbot.config;

import java.util.List;
import java.util.Optional;

public final class LocalTestAccounts {

    public static final String SESSION_USER_ID = "nyang.local-auth.user-id";
    public static final String SESSION_NICK_NAME = "nyang.local-auth.nick-name";
    public static final String SESSION_ADMIN = "nyang.local-auth.admin";

    public static final String ADMIN_USER_ID = "local-channel";
    public static final String VIEWER_USER_ID = "local-viewer";
    public static final String NEGATIVE_USER_ID = "user-006";

    private static final List<Account> ACCOUNTS = List.of(
            new Account(ADMIN_USER_ID, "로컬 관리자", true, "관리자 탭과 모든 운영 액션을 확인합니다."),
            new Account(VIEWER_USER_ID, "일반 시청자", false, "관리자 탭 없이 본인 랭킹과 내역만 확인합니다."),
            new Account(NEGATIVE_USER_ID, "이불밖은위험해", false, "음수 잔액과 권한 제한 상태를 확인합니다.")
    );

    private LocalTestAccounts() {
    }

    public static List<Account> accounts() {
        return ACCOUNTS;
    }

    public static Optional<Account> find(String userId) {
        if (userId == null || userId.isBlank()) {
            return Optional.empty();
        }
        return ACCOUNTS.stream()
                .filter(account -> account.userId().equals(userId))
                .findFirst();
    }

    public record Account(
            String userId,
            String nickName,
            boolean admin,
            String description
    ) {
    }
}
