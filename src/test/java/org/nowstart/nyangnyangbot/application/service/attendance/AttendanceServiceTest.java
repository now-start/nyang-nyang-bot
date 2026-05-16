package org.nowstart.nyangnyangbot.application.service.attendance;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.application.port.in.attendance.ManageAttendanceUseCase.AttendanceApplyCommand;
import org.nowstart.nyangnyangbot.application.port.in.attendance.ManageAttendanceUseCase.AttendanceApplyResult;
import org.nowstart.nyangnyangbot.application.port.in.attendance.ManageAttendanceUseCase.AttendanceUserSnapshot;
import org.nowstart.nyangnyangbot.application.port.in.favorite.AdjustFavoriteUseCase.AdjustFavoriteCommand;
import org.nowstart.nyangnyangbot.application.port.in.favorite.GrantFavoriteUseCase;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort.ChatEventPayload;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort.ChatEventPayload.Profile;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

    @Mock
    private GrantFavoriteUseCase grantFavoriteUseCase;

    @Test
    void recordChatUser_ShouldCollectAndSortUsersOnlyDuringActiveCycle() {
        // 준비
        AttendanceService attendanceService = new AttendanceService(grantFavoriteUseCase);
        attendanceService.recordChatUser(chat("ignored", "무시"));
        attendanceService.startCapture();
        attendanceService.recordChatUser(null);
        attendanceService.recordChatUser(new ChatEventPayload("channel", " ", profile("공백"), "hi", null, 1L));
        attendanceService.recordChatUser(chat("user-1", "치즈냥"));
        attendanceService.recordChatUser(chat("user-2", "후발냥"));

        // 실행
        List<AttendanceUserSnapshot> result = attendanceService.getActiveUsers();

        // 검증
        then(result).extracting(AttendanceUserSnapshot::userId)
                .containsExactlyInAnyOrder("user-2", "user-1");
        then(result).extracting(AttendanceUserSnapshot::nickName)
                .containsExactlyInAnyOrder("후발냥", "치즈냥");
    }

    @Test
    void applyAttendance_ShouldUseCapturedUsersAndDefaultAmount() {
        // 준비
        AttendanceService attendanceService = new AttendanceService(grantFavoriteUseCase);
        attendanceService.startCapture();
        attendanceService.recordChatUser(chat("user-1", "치즈냥"));

        // 실행
        AttendanceApplyResult result = attendanceService.applyAttendance(null);

        // 검증
        then(result.amount()).isEqualTo(1);
        then(result.count()).isEqualTo(1);
        BDDMockito.then(grantFavoriteUseCase).should().grant(argThat(command ->
                command.userId().equals("user-1")
                        && command.nickName().equals("치즈냥")
                        && command.delta() == 1
                        && command.publicDescription().equals("출석체크(+1)")
                        && command.createIfMissing()
                        && !command.allowNegativeBalance()
        ));
        then(attendanceService.getActiveUsers()).isEmpty();
    }

    @Test
    void applyAttendance_ShouldCloseCycleAfterReward() {
        // 준비
        AttendanceService attendanceService = new AttendanceService(grantFavoriteUseCase);
        AttendanceApplyCommand command = new AttendanceApplyCommand(
                List.of(new AttendanceUserSnapshot("user-1", "치즈냥", 1L)),
                3
        );

        attendanceService.startCapture();
        attendanceService.applyAttendance(command);

        // 실행 및 검증
        thenThrownBy(() -> attendanceService.applyAttendance(command))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("attendance cycle is not active");
        BDDMockito.then(grantFavoriteUseCase).should(BDDMockito.times(1)).grant(any(AdjustFavoriteCommand.class));
    }

    @Test
    void applyAttendance_ShouldRejectCanceledCycle() {
        // 준비
        AttendanceService attendanceService = new AttendanceService(grantFavoriteUseCase);
        attendanceService.startCapture();
        attendanceService.stopCapture();

        // 실행 및 검증
        thenThrownBy(() -> attendanceService.applyAttendance(new AttendanceApplyCommand(
                List.of(new AttendanceUserSnapshot("user-1", "치즈냥", 1L)),
                3
        )))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("attendance cycle is not active");
        BDDMockito.then(grantFavoriteUseCase).shouldHaveNoInteractions();
    }

    @Test
    void applyAttendance_ShouldRejectEmptyTargetsAndNonPositiveAmount() {
        // 준비
        AttendanceService attendanceService = new AttendanceService(grantFavoriteUseCase);
        attendanceService.startCapture();

        // 실행 및 검증
        thenThrownBy(() -> attendanceService.applyAttendance(new AttendanceApplyCommand(List.of(), 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("attendance targets are required");

        attendanceService.recordChatUser(chat("user-1", ""));
        thenThrownBy(() -> attendanceService.applyAttendance(new AttendanceApplyCommand(null, 0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("amount must be positive");
    }

    @Test
    void applyAttendance_ShouldSkipInvalidTargetAndUseSafeNickname() {
        // 준비
        AttendanceService attendanceService = new AttendanceService(grantFavoriteUseCase);
        attendanceService.startCapture();

        // 실행
        AttendanceApplyResult result = attendanceService.applyAttendance(new AttendanceApplyCommand(
                Arrays.asList(
                        null,
                        new AttendanceUserSnapshot("", "공백", 1L),
                        new AttendanceUserSnapshot("user-1", " ", 2L)
                ),
                2
        ));

        // 검증
        then(result.count()).isEqualTo(3);
        BDDMockito.then(grantFavoriteUseCase).should().grant(argThat(command ->
                command.userId().equals("user-1")
                        && command.nickName().equals("")
                        && command.delta() == 2
        ));
    }

    private ChatEventPayload chat(String userId, String nickName) {
        return new ChatEventPayload("channel", userId, profile(nickName), "hello", null, System.currentTimeMillis());
    }

    private Profile profile(String nickName) {
        return new Profile(nickName, List.of(), false);
    }
}
