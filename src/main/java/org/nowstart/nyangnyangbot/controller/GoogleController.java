package org.nowstart.nyangnyangbot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.service.GoogleSheetService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/google")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Google Sheet", description = "구글 시트 연동 및 데이터베이스 동기화 API")
public class GoogleController {

    private final GoogleSheetService googleSheetService;

    @Operation(
            summary = "데이터베이스 동기화",
            description = "구글 시트의 데이터를 데이터베이스와 동기화합니다. 매일 새벽 4시에 자동 실행됩니다."
    )
    @GetMapping(value = "/sync")
    public String syncDatabase() {
        try {
            log.info("[DBSync][START]");
            googleSheetService.updateFavorite();
            log.info("[DBSync][END]");
        } catch (Exception e) {
            log.error("[DBSync][ERROR] : ", e);
            throw new IllegalArgumentException(e.getMessage());
        }

        return "SUCCESS";
    }
}
