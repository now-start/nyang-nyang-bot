package org.nowstart.nyangnyangbot.adapter.in.web.attendance;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.adapter.in.web.attendance.request.AttendanceApplyRequest;
import org.nowstart.nyangnyangbot.adapter.in.web.attendance.response.AttendanceApplyResponse;
import org.nowstart.nyangnyangbot.adapter.in.web.attendance.response.AttendanceUserResponse;
import org.nowstart.nyangnyangbot.application.port.in.attendance.ManageAttendanceUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/attendance")
@Tag(name = "Attendance API", description = "출석체크 사용자 조회 및 적용 API")
@PreAuthorize("hasRole('ADMIN')")
public class AttendanceController {

    private final ManageAttendanceUseCase manageAttendanceUseCase;

    @Operation(summary = "현재 채팅 사용자 목록 조회")
    @GetMapping("/users")
    public ResponseEntity<List<AttendanceUserResponse>> getUsers() {
        return ResponseEntity.ok(manageAttendanceUseCase.getActiveUsers().stream()
                .map(AttendanceUserResponse::from)
                .toList());
    }

    @Operation(summary = "출석체크 수집 시작")
    @PostMapping("/start")
    public ResponseEntity<Void> startCapture() {
        manageAttendanceUseCase.startCapture();
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "출석체크 수집 종료")
    @PostMapping("/stop")
    public ResponseEntity<Void> stopCapture() {
        manageAttendanceUseCase.stopCapture();
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "출석체크 적용")
    @PostMapping("/apply")
    public ResponseEntity<AttendanceApplyResponse> applyAttendance(
            @RequestBody AttendanceApplyRequest request
    ) {
        return ResponseEntity.ok(AttendanceApplyResponse.from(
                manageAttendanceUseCase.applyAttendance(request.toApplyAttendanceCommand())
        ));
    }
}
