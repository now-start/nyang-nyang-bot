package org.nowstart.nyangnyangbot.adapter.in.web.attendance.request;

import java.util.List;
import org.nowstart.nyangnyangbot.application.port.in.attendance.dto.AttendanceApplyCommand;
import org.nowstart.nyangnyangbot.application.port.in.attendance.dto.AttendanceUserSnapshot;

public record AttendanceApplyRequest(
        List<AttendanceUser> users,
        Integer amount
) {

    public record AttendanceUser(String userId, String nickName, Long lastMessageTime) {

        AttendanceUserSnapshot toSnapshot() {
            return new AttendanceUserSnapshot(userId, nickName, lastMessageTime);
        }
    }

    public AttendanceApplyCommand toCommand() {
        List<AttendanceUserSnapshot> snapshots = users == null
                ? null
                : users.stream().map(AttendanceUser::toSnapshot).toList();
        return new AttendanceApplyCommand(snapshots, amount);
    }
}
