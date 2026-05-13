package org.nowstart.nyangnyangbot.application.port.in.attendance;

import java.util.List;

public interface ManageAttendanceUseCase {

    void startCapture();

    void stopCapture();

    List<AttendanceUserSnapshot> getActiveUsers();

    AttendanceApplyResult applyAttendance(AttendanceApplyCommand command);

    record AttendanceApplyCommand(
            List<AttendanceUserSnapshot> users,
            Integer amount
    ) {
    }

    record AttendanceApplyResult(
            Integer amount,
            Integer count
    ) {
    }

    record AttendanceUserSnapshot(
            String userId,
            String nickName,
            Long lastMessageTime
    ) {
    }
}
