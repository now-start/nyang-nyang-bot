package org.nowstart.chzzk_like_bot.scheduler;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.chzzk_like_bot.service.GoogleSheetService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DbSync {

    private final GoogleSheetService googleSheetService;
    private static final String RANGE = "호감도 순위표!B2:H2000";

    @Scheduled(cron = "0 0 4 * * ?") // Runs daily at 5 AM
    public void syncDatabase() {
        try {
            log.info("====================[START][DBSync]====================");
            List<List<Object>> rows = googleSheetService.getSheetValues(RANGE);
            googleSheetService.updateFavorite(rows);
            log.info("====================[END][DBSync]====================");
        } catch (Exception e) {
            log.error("====================[ERROR][DBSync]====================");
        }
    }
}