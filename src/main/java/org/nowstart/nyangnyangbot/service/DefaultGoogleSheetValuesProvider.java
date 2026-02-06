package org.nowstart.nyangnyangbot.service;

import com.google.api.services.sheets.v4.Sheets;
import com.google.auth.oauth2.GoogleCredentials;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.data.property.GoogleProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DefaultGoogleSheetValuesProvider {

    private static final String RANGE = "호감도 순위표!B2:H2000";
    private final GoogleProperty googleProperty;
    private final DefaultGoogleCredentialsProvider credentialsProvider;
    private final DefaultSheetsFactory sheetsFactory;

    public List<List<Object>> fetchValues() {
        GoogleCredentials credentials = credentialsProvider.getCredentials(googleProperty.getKey());
        Sheets sheets = sheetsFactory.create(credentials);
        try {
            return sheets.spreadsheets()
                    .values()
                    .get(googleProperty.getId(), RANGE)
                    .execute()
                    .getValues();
        } catch (IOException ex) {
            throw new IllegalStateException(ex.getMessage(), ex);
        }
    }
}
