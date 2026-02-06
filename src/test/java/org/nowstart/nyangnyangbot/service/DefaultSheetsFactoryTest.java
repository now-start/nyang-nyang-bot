package org.nowstart.nyangnyangbot.service;

import static org.assertj.core.api.BDDAssertions.then;

import com.google.api.services.sheets.v4.Sheets;
import com.google.auth.oauth2.GoogleCredentials;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DefaultSheetsFactoryTest {

    @Test
    void create_ShouldReturnSheetsInstance() {
        GoogleCredentials credentials = Mockito.mock(GoogleCredentials.class);
        DefaultSheetsFactory factory = new DefaultSheetsFactory();

        Sheets sheets = factory.create(credentials);

        then(sheets).isNotNull();
    }
}






