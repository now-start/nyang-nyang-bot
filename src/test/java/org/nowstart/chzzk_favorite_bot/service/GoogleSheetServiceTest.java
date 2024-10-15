package org.nowstart.chzzk_favorite_bot.service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import io.micrometer.common.util.StringUtils;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;

public class GoogleSheetServiceTest {
    private static final String APPLICATION_NAME = "google-sheet-project";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final List<String> SCOPES = Collections.singletonList(SheetsScopes.SPREADSHEETS);
    private static final String CREDENTIALS_FILE_PATH = "src/main/resources/key/google_spread_sheet_key.json";

    private static Credential getCredentials() throws IOException {
        FileInputStream credentialsStream = new FileInputStream(CREDENTIALS_FILE_PATH);
        return GoogleCredential.fromStream(credentialsStream)
            .createScoped(SCOPES);
    }

    public static void main(String[] args) throws IOException, GeneralSecurityException {
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

        String spreadSheetId = "1PKgmtFVrJWw4briZGxlfyKKUd3XaQscsmAGR6LZ12Os";
        String range = "호감도 순위표!B2:H2000";


        Sheets service = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials())
            .setApplicationName(APPLICATION_NAME)
            .build();

        List<List<Object>> rows = service.spreadsheets().values().get(spreadSheetId, range).execute().getValues();


        for (List<Object> row : rows) {
            String nickName = (String) row.get(0);
            String userId = (String) row.get(1);
            int totalFavorite = Integer.parseInt((String) row.get(row.size()-1));

            if(!StringUtils.isBlank(userId)){
                System.out.println("userId = " + userId);
                System.out.println("nickName = " + nickName);
                System.out.println("totalFavorite = " + totalFavorite);
            }
        }
    }
}