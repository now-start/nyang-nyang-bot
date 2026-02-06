package org.nowstart.nyangnyangbot.service;

import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.stereotype.Component;

@Component
public class DefaultSheetsFactory {

    public Sheets create(GoogleCredentials credentials) {
        return new Sheets.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance(), new HttpCredentialsAdapter(credentials))
                .setApplicationName("google-sheet-project")
                .build();
    }
}






