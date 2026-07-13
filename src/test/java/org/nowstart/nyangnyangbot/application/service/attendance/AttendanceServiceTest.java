package org.nowstart.nyangnyangbot.application.service.attendance;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.willThrow;
import static org.nowstart.nyangnyangbot.support.MethodValidationTestSupport.validated;

import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.application.port.in.chzzk.HandleChzzkEventUseCase.ChatReceived;
import org.nowstart.nyangnyangbot.application.port.in.chzzk.HandleChzzkEventUseCase.ChatReceived.Profile;
import org.nowstart.nyangnyangbot.application.port.in.attendance.ManageAttendanceUseCase.AttendanceApplyCommand;
import org.nowstart.nyangnyangbot.application.port.in.attendance.ManageAttendanceUseCase.AttendanceApplyResult;
import org.nowstart.nyangnyangbot.application.port.in.attendance.ManageAttendanceUseCase.AttendanceUserSnapshot;
import org.nowstart.nyangnyangbot.application.port.in.favorite.AdjustFavoriteUseCase.AdjustFavoriteCommand;
import org.nowstart.nyangnyangbot.application.port.in.favorite.GrantFavoriteUseCase;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

    @Mock
    private GrantFavoriteUseCase grantFavoriteUseCase;

    @Test
    void recordChatUser_ShouldCollectAndSortUsersOnlyDuringActiveCycle() {
        // 준비
        AttendanceService attendanceService = service();
        attendanceService.recordChatUser(chat("ignored", "무시"));
        attendanceService.startCapture();
        attendanceService.recordChatUser(null);
        attendanceService.recordChatUser(new ChatReceived("channel", " ", profile("공백"), "hi", null, 1L));
        attendanceService.recordChatUser(chat("user-1", "치즈냥"));
        attendanceService.recordChatUser(chat("user-2", "후발냥"));
        attendanceService.recordChatUser(new ChatReceived("channel", "user-3", null, "hi", null, 1L));

        // 실행
        List<AttendanceUserSnapshot> result = attendanceService.getActiveUsers();

        // 검증
        then(result).extracting(AttendanceUserSnapshot::userId)
                .containsExactlyInAnyOrder("user-3", "user-2", "user-1");
        then(result).extracting(AttendanceUserSnapshot::nickName)
                .containsExactlyInAnyOrder("user-3", "후발냥", "치즈냥");
    }

    @Test
    void applyAttendance_ShouldUseSelectedCapturedUsers() {
        // 준비
        AttendanceService attendanceService = service();
        attendanceService.startCapture();
        attendanceService.recordChatUser(chat("user-1", "치즈냥"));

        // 실행
        AttendanceApplyResult result = attendanceService.applyAttendance(
                new AttendanceApplyCommand(List.of("user-1"), 1)
        );

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
        AttendanceService attendanceService = service();
        AttendanceApplyCommand command = new AttendanceApplyCommand(
                List.of("user-1"),
                3
        );

        attendanceService.startCapture();
        attendanceService.recordChatUser(chat("user-1", "치즈냥"));
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
        AttendanceService attendanceService = service();
        attendanceService.startCapture();
        attendanceService.stopCapture();

        // 실행 및 검증
        thenThrownBy(() -> attendanceService.applyAttendance(new AttendanceApplyCommand(
                List.of("user-1"),
                3
        )))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("attendance cycle is not active");
        BDDMockito.then(grantFavoriteUseCase).shouldHaveNoInteractions();
    }

    @Test
    void applyAttendance_ShouldRejectEmptyTargetsAndNonPositiveAmount() {
        // 준비
        AttendanceService attendanceService = service();
        attendanceService.startCapture();

        // 실행 및 검증
        thenThrownBy(() -> attendanceService.applyAttendance(new AttendanceApplyCommand(List.of(), 1)))
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("attendance targets are required");

        attendanceService.recordChatUser(chat("user-1", ""));
        thenThrownBy(() -> attendanceService.applyAttendance(new AttendanceApplyCommand(List.of("user-1"), 0)))
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("amount must be positive");
    }

    @Test
    void applyAttendance_ShouldRejectInvalidExplicitTarget() {
        // 준비
        AttendanceService attendanceService = service();
        attendanceService.startCapture();

        // 실행 및 검증
        thenThrownBy(() -> attendanceService.applyAttendance(new AttendanceApplyCommand(
                List.of(""),
                2
        )))
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("userId is required");
        BDDMockito.then(grantFavoriteUseCase).shouldHaveNoInteractions();
    }

    @Test
    void applyAttendance_ShouldRejectNullExplicitTarget() {
        // 준비
        AttendanceService attendanceService = service();
        attendanceService.startCapture();

        // 실행 및 검증
        thenThrownBy(() -> attendanceService.applyAttendance(new AttendanceApplyCommand(
                java.util.Arrays.asList((String) null),
                2
        )))
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("userId is required");
        BDDMockito.then(grantFavoriteUseCase).shouldHaveNoInteractions();
    }

    @Test
    void applyAttendance_ShouldResolveSelectedUserFromActiveCycle() {
        // 준비
        AttendanceService attendanceService = service();
        attendanceService.startCapture();
        attendanceService.recordChatUser(chat("user-1", "서버닉네임"));

        // 실행
        attendanceService.applyAttendance(new AttendanceApplyCommand(List.of("user-1"), 2));

        // 검증
        BDDMockito.then(grantFavoriteUseCase).should().grant(argThat(command ->
                command.userId().equals("user-1") && command.nickName().equals("서버닉네임")
        ));
    }

    @Test
    void applyAttendance_ShouldCloseCycleBeforeGranting() {
        // 준비
        AttendanceService attendanceService = service();
        attendanceService.startCapture();
        attendanceService.recordChatUser(chat("user-1", "치즈냥"));
        willThrow(new IllegalStateException("grant failed"))
                .given(grantFavoriteUseCase)
                .grant(any(AdjustFavoriteCommand.class));

        // 실행 및 검증
        thenThrownBy(() -> attendanceService.applyAttendance(new AttendanceApplyCommand(List.of("user-1"), 2)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("grant failed");
        thenThrownBy(() -> attendanceService.applyAttendance(new AttendanceApplyCommand(List.of("user-1"), 2)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("attendance cycle is not active");
    }

    private ChatReceived chat(String userId, String nickName) {
        return new ChatReceived("channel", userId, profile(nickName), "hello", null, System.currentTimeMillis());
    }

    private Profile profile(String nickName) {
        return new Profile(nickName, List.of(), false);
    }

    private AttendanceService service() {
        return validated(new AttendanceService(grantFavoriteUseCase));
    }
}
