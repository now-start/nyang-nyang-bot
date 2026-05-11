package org.nowstart.nyangnyangbot.application.port.in.attendance.dto;

import java.util.List;

public record AttendanceApplyCommand(
        List<AttendanceUserSnapshot> users,
        Integer amount
) {
}
