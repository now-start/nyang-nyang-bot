package org.nowstart.nyangnyangbot.service;

import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.auth.oauth2.GoogleCredentials;
import java.io.FileInputStream;
import java.io.IOException;
import org.springframework.stereotype.Component;

@Component
public class DefaultGoogleCredentialsProvider {

    public GoogleCredentials getCredentials(String keyPath) {
        try (FileInputStream inputStream = new FileInputStream(keyPath)) {
            return GoogleCredentials.fromStream(inputStream)
                    .createScoped(SheetsScopes.SPREADSHEETS);
        } catch (IOException ex) {
            throw new IllegalArgumentException(ex.getMessage(), ex);
        }
    }
}






