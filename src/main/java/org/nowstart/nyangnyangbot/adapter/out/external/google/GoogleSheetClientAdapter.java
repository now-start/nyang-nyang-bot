package org.nowstart.nyangnyangbot.adapter.out.external.google;

import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import java.io.FileInputStream;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.adapter.out.external.google.response.GoogleSheetRowResponse;
import org.nowstart.nyangnyangbot.application.port.out.google.GoogleSheetPort;
import org.nowstart.nyangnyangbot.application.port.out.google.GoogleSheetPort.GoogleSheetRow;
import org.nowstart.nyangnyangbot.config.property.GoogleProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleSheetClientAdapter implements GoogleSheetPort {

    private static final String RANGE = "호감도 순위표!B2:H2000";

    private final GoogleProperty googleProperty;

    @Override
    @SneakyThrows
    public List<GoogleSheetRow> readFavoriteRows() {
        GoogleCredentials credentials =
                GoogleCredentials.fromStream(new FileInputStream(googleProperty.key())).createScoped(SheetsScopes.SPREADSHEETS);
        Sheets sheets = new Sheets.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance(), new HttpCredentialsAdapter(credentials))
                .setApplicationName("google-sheet-project")
                .build();

        List<List<Object>> values = sheets.spreadsheets()
                .values()
                .get(googleProperty.id(), RANGE)
                .execute().getValues();

        return values.stream()
                .map(value -> {
                    log.info("[GoogleSheet] Row value: {}", value);
                    return new GoogleSheetRowResponse(value).toGoogleSheetRow();
                })
                .toList();
    }
}
