package org.nowstart.nyangnyangbot.config;

import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.service.GoogleSheetService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class GoogleConfig {

    private static final String RANGE = "호감도 순위표!B2:H2000";
    @Value("${google.spreadsheet.key}")
    private String credentialsFilePath;
    @Value("${google.spreadsheet.id}")
    private String spreadSheetId;
    private final GoogleSheetService googleSheetService;

    @Scheduled(cron = "0 0 4 * * ?") // Runs daily at 4 AM
    public void syncDatabase() {
        try {
            log.info("[DBSync][START]");
            List<List<Object>> rows = getSheetValues();
            googleSheetService.updateFavorite(rows);
            log.info("[DBSync][END]");
        } catch (Exception e) {
            log.error("[DBSync][ERROR]", e);
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    private List<List<Object>> getSheetValues() throws IOException {
        GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream(credentialsFilePath)).createScoped(Collections.singletonList(SheetsScopes.SPREADSHEETS));
        Sheets sheets = new Sheets.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance(), new HttpCredentialsAdapter(credentials))
            .setApplicationName("google-sheet-project")
            .build();

        return sheets.spreadsheets()
            .values()
            .get(spreadSheetId, RANGE)
            .execute().getValues();
    }
}
