package org.nowstart.nyangnyangbot.adapter.out.external.google;

import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.adapter.out.external.google.response.GoogleSheetRowResponse;
import org.nowstart.nyangnyangbot.application.exception.ExternalSystemException;
import org.nowstart.nyangnyangbot.application.port.out.google.GoogleSheetPort;
import org.nowstart.nyangnyangbot.config.property.GoogleProperty;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Validated
@RequiredArgsConstructor
public class GoogleSheetClientAdapter implements GoogleSheetPort {

    private static final String RANGE = "호감도 순위표!B2:H";

    private final GoogleProperty googleProperty;

    @Override
    public List<GoogleSheetRow> readPointRows() {
        List<List<Object>> values;
        try {
            GoogleCredentials credentials = loadCredentials();
            Sheets sheets = new Sheets.Builder(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance(),
                    new HttpCredentialsAdapter(credentials)
            )
                    .setApplicationName("google-sheet-project")
                    .build();

            values = sheets.spreadsheets()
                    .values()
                    .get(googleProperty.id(), RANGE)
                    .execute().getValues();
        } catch (IOException exception) {
            throw new ExternalSystemException("Google Sheets API request failed", exception);
        }

        return toRows(values);
    }

    GoogleCredentials loadCredentials() throws IOException {
        String keyJson = googleProperty.key();
        if (keyJson == null || keyJson.isBlank()) {
            throw new IOException("Google service account JSON is required");
        }
        try (InputStream stream = new ByteArrayInputStream(keyJson.getBytes(StandardCharsets.UTF_8))) {
            return ServiceAccountCredentials.fromStream(stream).createScoped(SheetsScopes.SPREADSHEETS);
        } catch (IOException | IllegalArgumentException exception) {
            // Parser errors may contain credential data; do not retain the original cause.
            throw new IOException("Invalid Google service account JSON");
        }
    }

    List<GoogleSheetRow> toRows(List<List<Object>> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .map(GoogleSheetRowResponse::new)
                .flatMap(response -> response.toGoogleSheetRow().stream())
                .toList();
    }
}
