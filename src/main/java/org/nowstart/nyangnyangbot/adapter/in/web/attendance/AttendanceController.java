package org.nowstart.nyangnyangbot.adapter.in.web.attendance;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.application.port.in.attendance.ManageAttendanceUseCase;
import org.nowstart.nyangnyangbot.application.port.in.attendance.ManageAttendanceUseCase.AttendanceApplyCommand;
import org.nowstart.nyangnyangbot.application.port.in.attendance.ManageAttendanceUseCase.AttendanceUserSnapshot;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/attendance")
@Tag(name = "Attendance API", description = "출석체크 사용자 조회 및 적용 API")
@PreAuthorize("hasRole('ADMIN')")
public class AttendanceController {

    private static final String ATTENDANCE_LIST_FRAGMENT = "features/attendance/components :: attendance-list";
    private static final String FEEDBACK_FRAGMENT = "features/attendance/components :: attendance-feedback-response";
    private static final String USERS_REFRESH_TRIGGER = "attendance-users-refresh";
    private static final String APPLY_SUCCESS_TRIGGER =
            "{\"favorite-board-refresh\":{}}";

    private final ManageAttendanceUseCase manageAttendanceUseCase;

    @Operation(summary = "현재 채팅 사용자 목록 조회")
    @RequestMapping(path = "/users", method = {RequestMethod.GET, RequestMethod.POST})
    public String getUsers(
            @RequestParam(required = false) List<String> userIds,
            @RequestParam(required = false) List<String> knownUserIds,
            @RequestParam(defaultValue = "false") boolean selectionInitialized,
            Model model
    ) {
        addUsers(model, userIds, knownUserIds, selectionInitialized);
        return ATTENDANCE_LIST_FRAGMENT;
    }

    @Operation(summary = "출석체크 수집 시작")
    @PostMapping("/start")
    public String startCapture(HttpServletResponse response, Model model) {
        manageAttendanceUseCase.startCapture();
        model.addAttribute("message", "출석체크 수집 시작");
        model.addAttribute("tone", "success");
        model.addAttribute("resetAttendanceList", false);
        response.addHeader("HX-Trigger", USERS_REFRESH_TRIGGER);
        return FEEDBACK_FRAGMENT;
    }

    @Operation(summary = "출석체크 수집 종료")
    @PostMapping("/stop")
    public String stopCapture(Model model) {
        manageAttendanceUseCase.stopCapture();
        model.addAttribute("message", "출석체크 수집 종료");
        model.addAttribute("tone", "secondary");
        model.addAttribute("resetAttendanceList", true);
        return FEEDBACK_FRAGMENT;
    }

    @Operation(summary = "출석체크 적용")
    @PostMapping("/apply")
    public String applyAttendance(
            @ModelAttribute AttendanceApplyForm form,
            HttpServletResponse response,
            Model model
    ) {
        try {
            if (form.amount() == null || form.amount() <= 0) {
                throw new IllegalArgumentException("amount must be positive");
            }
            List<AttendanceUserSnapshot> users = selectedUsers(form.userIds());
            manageAttendanceUseCase.applyAttendance(new AttendanceApplyCommand(users, form.amount()));
            model.addAttribute("message", "출석체크 완료");
            model.addAttribute("tone", "success");
            model.addAttribute("resetAttendanceList", true);
            response.addHeader("HX-Trigger", APPLY_SUCCESS_TRIGGER);
        } catch (RuntimeException e) {
            log.warn("Failed to apply attendance.", e);
            model.addAttribute("message", "출석체크 실패");
            model.addAttribute("tone", "danger");
            model.addAttribute("resetAttendanceList", false);
        }
        return FEEDBACK_FRAGMENT;
    }

    private void addUsers(
            Model model,
            List<String> selectedUserIds,
            List<String> knownUserIds,
            boolean selectionInitialized
    ) {
        List<AttendanceUserView> users = manageAttendanceUseCase.getActiveUsers().stream()
                .map(AttendanceUserView::from)
                .toList();
        Set<String> selectedIds = selectionInitialized
                ? (selectedUserIds == null ? Set.of() : new HashSet<>(selectedUserIds))
                : null;
        Set<String> knownIds = selectionInitialized
                ? (knownUserIds == null ? Set.of() : new HashSet<>(knownUserIds))
                : Set.of();
        long selectedCount = selectedIds == null
                ? users.size()
                : users.stream()
                        .filter(user -> selectedIds.contains(user.userId()) || !knownIds.contains(user.userId()))
                        .count();
        model.addAttribute("users", users);
        model.addAttribute("selectedUserIds", selectedIds);
        model.addAttribute("knownUserIds", knownIds);
        model.addAttribute("selectedCount", selectedCount);
        model.addAttribute("totalCount", users.size());
    }

    private List<AttendanceUserSnapshot> selectedUsers(List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            throw new IllegalArgumentException("attendance targets are required");
        }
        List<AttendanceUserSnapshot> users = manageAttendanceUseCase.getActiveUsers().stream()
                .filter(user -> userIds.contains(user.userId()))
                .toList();
        if (users.isEmpty()) {
            throw new IllegalArgumentException("attendance targets are required");
        }
        return users;
    }

    public record AttendanceApplyForm(
            Integer amount,
            List<String> userIds
    ) {
    }

    public record AttendanceUserView(
            String userId,
            String nickName,
            Long lastMessageTime
    ) {

        static AttendanceUserView from(AttendanceUserSnapshot snapshot) {
            return new AttendanceUserView(snapshot.userId(), snapshot.nickName(), snapshot.lastMessageTime());
        }
    }
}
