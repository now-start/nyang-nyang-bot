package org.nowstart.nyangnyangbot.adapter.in.web.attendance.response;

import org.nowstart.nyangnyangbot.application.port.in.attendance.dto.AttendanceUserSnapshot;

public record AttendanceUserResponse(
        String userId,
        String nickName,
        Long lastMessageTime
) {

    public static AttendanceUserResponse from(AttendanceUserSnapshot snapshot) {
        return new AttendanceUserResponse(snapshot.userId(), snapshot.nickName(), snapshot.lastMessageTime());
    }
}
