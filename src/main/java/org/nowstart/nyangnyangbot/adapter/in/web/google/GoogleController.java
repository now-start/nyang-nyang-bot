package org.nowstart.nyangnyangbot.adapter.in.web.google;

import jakarta.servlet.http.HttpServletResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.application.port.in.google.SyncGoogleSheetUseCase;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/google")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Google Sheet", description = "구글 시트 연동 및 데이터베이스 동기화 API")
public class GoogleController {

    private static final String SYNC_FEEDBACK_FRAGMENT = "components/feedback :: alert";
    private static final String POINT_BOARD_REFRESH_TRIGGER = "point-board-refresh";

    private final SyncGoogleSheetUseCase syncGoogleSheetUseCase;

    @Operation(
            summary = "데이터베이스 동기화",
            description = "구글 시트의 데이터를 데이터베이스와 동기화하고 htmx feedback fragment를 반환합니다."
    )
    @PostMapping("/sync")
    public String syncDatabase(HttpServletResponse response, Model model) {
        try {
            runSync();
            model.addAttribute("message", "데이터 동기화 완료");
            model.addAttribute("tone", "success");
            response.addHeader("HX-Trigger", POINT_BOARD_REFRESH_TRIGGER);
        } catch (RuntimeException e) {
            log.warn("[DBSync][FAILED]", e);
            model.addAttribute("message", "데이터 동기화 실패");
            model.addAttribute("tone", "danger");
        }
        return SYNC_FEEDBACK_FRAGMENT;
    }

    private void runSync() {
        log.info("[DBSync][START]");
        syncGoogleSheetUseCase.synchronizePoints();
        log.info("[DBSync][END]");
    }
}
