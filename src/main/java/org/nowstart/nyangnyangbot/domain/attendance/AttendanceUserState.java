package org.nowstart.nyangnyangbot.domain.attendance;

public record AttendanceUserState(
        String userId,
        String nickName,
        long lastMessageTime
) {
}
