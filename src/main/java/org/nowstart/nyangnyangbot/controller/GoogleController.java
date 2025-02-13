package org.nowstart.nyangnyangbot.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.service.GoogleSheetService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class GoogleController {

    private final GoogleSheetService googleSheetService;

    @Scheduled(cron = "0 0 4 * * ?")
    @GetMapping(value = "/sync")
    public String syncDatabase() {
        try {
            log.info("[DBSync][START]");
            googleSheetService.updateFavorite();
            log.info("[DBSync][END]");
        } catch (Exception e) {
            log.error("[DBSync][ERROR]", e);
            throw new IllegalArgumentException(e.getMessage());
        }

        return "SUCCESS";
    }
}
