package org.nowstart.chzzk_like_bot.config;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class GoogleConfig {
    private static final String APPLICATION_NAME = "google-sheet-project";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final List<String> SCOPES = Collections.singletonList(SheetsScopes.SPREADSHEETS);
    private static final String CREDENTIALS_FILE_PATH = "src/main/resources/key/google_spread_sheet_key.json";

    private static Credential getCredentials() throws IOException {
        FileInputStream credentialsStream = new FileInputStream(CREDENTIALS_FILE_PATH);
        return GoogleCredential.fromStream(credentialsStream)
            .createScoped(SCOPES);
    }

    @Bean
    public Sheets sheetsService() throws GeneralSecurityException, IOException {
        return new Sheets.Builder(GoogleNetHttpTransport.newTrustedTransport(), JSON_FACTORY, getCredentials())
            .setApplicationName(APPLICATION_NAME)
            .build();
    }
}
