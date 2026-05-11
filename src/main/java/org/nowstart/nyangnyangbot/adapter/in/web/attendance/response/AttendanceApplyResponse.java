package org.nowstart.nyangnyangbot.adapter.in.web.attendance.response;

import org.nowstart.nyangnyangbot.application.port.in.attendance.dto.AttendanceApplyResult;

public record AttendanceApplyResponse(
        Integer amount,
        Integer count
) {

    public static AttendanceApplyResponse from(AttendanceApplyResult result) {
        return new AttendanceApplyResponse(result.amount(), result.count());
    }
}
