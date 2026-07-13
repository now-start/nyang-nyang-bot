package org.nowstart.nyangnyangbot.application.port.in.attendance;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import java.util.List;

public interface ManageAttendanceUseCase {

    void startCapture();

    void stopCapture();

    List<AttendanceUserSnapshot> getActiveUsers();

    AttendanceApplyResult applyAttendance(
            @Valid @NotNull(message = "command is required") AttendanceApplyCommand command
    );

    record AttendanceApplyCommand(
            @NotEmpty(message = "attendance targets are required")
            List<@NotBlank(message = "userId is required") String> userIds,
            @NotNull(message = "amount is required")
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
