package org.nowstart.nyangnyangbot.adapter.in.web.attendance;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.adapter.in.web.attendance.AttendanceController.AttendanceApplyForm;
import org.nowstart.nyangnyangbot.application.port.in.attendance.ManageAttendanceUseCase;
import org.nowstart.nyangnyangbot.application.port.in.attendance.ManageAttendanceUseCase.AttendanceApplyCommand;
import org.nowstart.nyangnyangbot.application.port.in.attendance.ManageAttendanceUseCase.AttendanceApplyResult;
import org.nowstart.nyangnyangbot.application.port.in.attendance.ManageAttendanceUseCase.AttendanceUserSnapshot;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.ui.ExtendedModelMap;

@ExtendWith(MockitoExtension.class)
class AttendanceControllerTest {

    @Mock
    private ManageAttendanceUseCase manageAttendanceUseCase;

    @InjectMocks
    private AttendanceController controller;

    @Test
    void getUsers_ShouldReturnAttendanceListFragment() {
        // 준비
        given(manageAttendanceUseCase.getActiveUsers())
                .willReturn(List.of(new AttendanceUserSnapshot("user1", "치즈냥", 1L)));
        ExtendedModelMap model = new ExtendedModelMap();

        // 실행
        String view = controller.getUsers(null, null, false, model);

        // 검증
        then(view).isEqualTo("features/attendance/components :: attendance-list");
        then((List<?>) model.get("users")).hasSize(1);
        then(model.get("selectedCount")).isEqualTo(1L);
        then(model.get("totalCount")).isEqualTo(1);
    }

    @Test
    void getUsers_ShouldPreserveSelectedUsersWhenSelectionInitialized() {
        // 준비
        given(manageAttendanceUseCase.getActiveUsers()).willReturn(List.of(
                new AttendanceUserSnapshot("user1", "치즈냥", 1L),
                new AttendanceUserSnapshot("user2", "후발냥", 2L),
                new AttendanceUserSnapshot("user3", "신규냥", 3L)
        ));
        ExtendedModelMap model = new ExtendedModelMap();

        // 실행
        String view = controller.getUsers(List.of("user2"), List.of("user1", "user2"), true, model);

        // 검증
        then(view).isEqualTo("features/attendance/components :: attendance-list");
        then((List<?>) model.get("users")).hasSize(3);
        then(model.get("selectedUserIds")).isEqualTo(java.util.Set.of("user2"));
        then(model.get("knownUserIds")).isEqualTo(java.util.Set.of("user1", "user2"));
        then(model.get("selectedCount")).isEqualTo(2L);
        then(model.get("totalCount")).isEqualTo(3);
    }

    @Test
    void startCapture_ShouldReturnFeedbackFragment() {
        // 준비
        MockHttpServletResponse response = new MockHttpServletResponse();
        ExtendedModelMap model = new ExtendedModelMap();

        // 실행
        String view = controller.startCapture(response, model);

        // 검증
        then(view).isEqualTo("features/attendance/components :: attendance-feedback-response");
        then(model.get("tone")).isEqualTo("success");
        then(model.get("resetAttendanceList")).isEqualTo(false);
        then(response.getHeader("HX-Trigger")).isEqualTo("attendance-users-refresh");
        org.mockito.BDDMockito.then(manageAttendanceUseCase).should().startCapture();
    }

    @Test
    void applyAttendance_ShouldUseSelectedActiveUsersAndReturnRefreshTrigger() {
        // 준비
        given(manageAttendanceUseCase.getActiveUsers()).willReturn(List.of(
                new AttendanceUserSnapshot("user1", "치즈냥", 1L),
                new AttendanceUserSnapshot("user2", "후발냥", 2L)
        ));
        given(manageAttendanceUseCase.applyAttendance(any(AttendanceApplyCommand.class)))
                .willReturn(new AttendanceApplyResult(5, 1));
        MockHttpServletResponse response = new MockHttpServletResponse();
        ExtendedModelMap model = new ExtendedModelMap();

        // 실행
        String view = controller.applyAttendance(new AttendanceApplyForm(5, List.of("user2")), response, model);

        // 검증
        then(view).isEqualTo("features/attendance/components :: attendance-feedback-response");
        then(model.get("tone")).isEqualTo("success");
        then(model.get("resetAttendanceList")).isEqualTo(true);
        then(response.getHeader("HX-Trigger")).contains("favorite-board-refresh");

        ArgumentCaptor<AttendanceApplyCommand> captor = ArgumentCaptor.forClass(AttendanceApplyCommand.class);
        org.mockito.BDDMockito.then(manageAttendanceUseCase).should().applyAttendance(captor.capture());
        then(captor.getValue().amount()).isEqualTo(5);
        then(captor.getValue().users()).extracting(AttendanceUserSnapshot::userId).containsExactly("user2");
    }

    @Test
    void applyAttendance_ShouldRejectEmptySelectionWithoutFallingBackToAllUsers() {
        // 준비
        given(manageAttendanceUseCase.getActiveUsers())
                .willReturn(List.of(new AttendanceUserSnapshot("user1", "치즈냥", 1L)));
        MockHttpServletResponse response = new MockHttpServletResponse();
        ExtendedModelMap model = new ExtendedModelMap();

        // 실행
        String view = controller.applyAttendance(new AttendanceApplyForm(5, List.of("missing")), response, model);

        // 검증
        then(view).isEqualTo("features/attendance/components :: attendance-feedback-response");
        then(model.get("tone")).isEqualTo("danger");
        then(model.get("resetAttendanceList")).isEqualTo(false);
        org.mockito.BDDMockito.then(manageAttendanceUseCase).should(org.mockito.Mockito.never()).applyAttendance(any());
    }
}
