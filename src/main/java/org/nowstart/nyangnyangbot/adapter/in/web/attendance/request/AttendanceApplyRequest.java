package org.nowstart.nyangnyangbot.adapter.in.web.attendance.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.nowstart.nyangnyangbot.application.port.in.attendance.ManageAttendanceUseCase.AttendanceApplyCommand;
import org.nowstart.nyangnyangbot.application.port.in.attendance.ManageAttendanceUseCase.AttendanceUserSnapshot;

public record AttendanceApplyRequest(
        List<@NotNull(message = "attendance target is required") @Valid AttendanceUser> users,
        @Positive(message = "amount must be positive")
        Integer amount
) {

    public AttendanceApplyCommand toApplyAttendanceCommand() {
        List<AttendanceUserSnapshot> snapshots = users == null
                ? null
                : users.stream().map(AttendanceUser::toSnapshot).toList();
        return new AttendanceApplyCommand(snapshots, amount);
    }

    public record AttendanceUser(
            @NotBlank(message = "userId is required")
            String userId,
            String nickName,
            Long lastMessageTime
    ) {

        AttendanceUserSnapshot toSnapshot() {
            return new AttendanceUserSnapshot(userId, nickName, lastMessageTime);
        }
    }
}
