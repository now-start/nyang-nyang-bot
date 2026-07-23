package org.nowstart.nyangnyangbot.adapter.out.external.google;

import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.nowstart.nyangnyangbot.adapter.out.external.google.response.GoogleSheetRowResponse;
import org.nowstart.nyangnyangbot.adapter.out.validation.OutboundContractValidator;
import org.nowstart.nyangnyangbot.application.port.out.google.GoogleSheetPort;
import org.nowstart.nyangnyangbot.config.property.GoogleProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GoogleSheetClientAdapter implements GoogleSheetPort {

    private static final String RANGE = "호감도 순위표!B2:H2000";

    private final GoogleProperty googleProperty;
    private final OutboundContractValidator contractValidator;

    @Override
    @SneakyThrows
    public List<GoogleSheetRow> readPointRows() {
        GoogleCredentials credentials = loadCredentials();
        Sheets sheets = new Sheets.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance(), new HttpCredentialsAdapter(credentials))
                .setApplicationName("google-sheet-project")
                .build();

        List<List<Object>> values = sheets.spreadsheets()
                .values()
                .get(googleProperty.id(), RANGE)
                .execute().getValues();

        return toRows(values);
    }

    private GoogleCredentials loadCredentials() throws IOException {
        try (InputStream keyStream = Files.newInputStream(Path.of(googleProperty.key()))) {
            return GoogleCredentials.fromStream(keyStream).createScoped(SheetsScopes.SPREADSHEETS);
        }
    }

    List<GoogleSheetRow> toRows(List<List<Object>> values) {
        if (values == null) {
            return List.of();
        }
        List<GoogleSheetRow> rows = values.stream()
                .map(GoogleSheetRowResponse::new)
                .flatMap(response -> response.toGoogleSheetRow().stream())
                .toList();
        for (int index = 0; index < rows.size(); index++) {
            contractValidator.externalResponse("googleSheet.readPointRows[" + index + "]", rows.get(index));
        }
        return rows;
    }
}
