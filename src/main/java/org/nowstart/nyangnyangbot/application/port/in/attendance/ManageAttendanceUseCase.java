package org.nowstart.nyangnyangbot.application.port.in.attendance;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

public interface ManageAttendanceUseCase {

    void startCapture();

    void stopCapture();

    List<AttendanceUserSnapshot> getActiveUsers();

    AttendanceApplyResult applyAttendance(AttendanceApplyCommand command);

    record AttendanceApplyCommand(
            List<@NotNull(message = "attendance target is required") @Valid AttendanceUserSnapshot> users,
            @Positive(message = "amount must be positive")
            Integer amount
    ) {
    }

    record AttendanceApplyResult(
            Integer amount,
            Integer count
    ) {
    }

    record AttendanceUserSnapshot(
            @NotBlank(message = "userId is required")
            String userId,
            String nickName,
            Long lastMessageTime
    ) {
    }
}
