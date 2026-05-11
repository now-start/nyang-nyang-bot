package org.nowstart.nyangnyangbot.application.service.attendance;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.application.port.in.attendance.dto.AttendanceApplyCommand;
import org.nowstart.nyangnyangbot.application.port.in.attendance.dto.AttendanceUserSnapshot;
import org.nowstart.nyangnyangbot.application.port.in.favorite.dto.AdjustFavoriteCommand;
import org.nowstart.nyangnyangbot.application.port.in.favorite.usecase.GrantFavoriteUseCase;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

    @Mock
    private GrantFavoriteUseCase grantFavoriteUseCase;

    @Test
    void applyAttendance_ShouldCloseCycleAfterReward() {
        AttendanceService attendanceService = new AttendanceService(grantFavoriteUseCase);
        AttendanceApplyCommand command = new AttendanceApplyCommand(
                List.of(new AttendanceUserSnapshot("user-1", "치즈냥", 1L)),
                3
        );

        attendanceService.startCapture();
        attendanceService.applyAttendance(command);

        assertThatThrownBy(() -> attendanceService.applyAttendance(command))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("attendance cycle is not active");
        BDDMockito.then(grantFavoriteUseCase).should(BDDMockito.times(1)).grant(any(AdjustFavoriteCommand.class));
    }

    @Test
    void applyAttendance_ShouldRejectCanceledCycle() {
        AttendanceService attendanceService = new AttendanceService(grantFavoriteUseCase);
        attendanceService.startCapture();
        attendanceService.stopCapture();

        assertThatThrownBy(() -> attendanceService.applyAttendance(new AttendanceApplyCommand(
                List.of(new AttendanceUserSnapshot("user-1", "치즈냥", 1L)),
                3
        )))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("attendance cycle is not active");
        BDDMockito.then(grantFavoriteUseCase).shouldHaveNoInteractions();
    }
}
