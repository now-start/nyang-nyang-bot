package org.nowstart.nyangnyangbot.application.port.in.attendance.dto;

public record AttendanceUserSnapshot(
        String userId,
        String nickName,
        Long lastMessageTime
) {
}
