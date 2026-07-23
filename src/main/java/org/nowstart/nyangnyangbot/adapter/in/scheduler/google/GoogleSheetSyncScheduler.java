package org.nowstart.nyangnyangbot.adapter.in.scheduler.google;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.application.port.in.google.SyncGoogleSheetUseCase;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "google.spreadsheet.sync", name = "enabled", havingValue = "true", matchIfMissing = true)
public class GoogleSheetSyncScheduler {

    private final SyncGoogleSheetUseCase syncGoogleSheetUseCase;

    @Scheduled(cron = "0 0 4 * * ?")
    public void syncDatabase() {
        log.info("[DBSync][START]");
        syncGoogleSheetUseCase.synchronizePoints();
        log.info("[DBSync][END]");
    }
}
