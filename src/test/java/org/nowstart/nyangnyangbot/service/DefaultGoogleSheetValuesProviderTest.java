package org.nowstart.nyangnyangbot.service;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import static org.mockito.BDDMockito.given;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.oauth2.GoogleCredentials;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.data.property.GoogleProperty;

@ExtendWith(MockitoExtension.class)
class DefaultGoogleSheetValuesProviderTest {

    @Mock
    private GoogleProperty googleProperty;

    @Mock
    private DefaultGoogleCredentialsProvider credentialsProvider;

    @Mock
    private DefaultSheetsFactory sheetsFactory;

    @Mock
    private GoogleCredentials credentials;

    @Mock
    private Sheets sheets;

    @InjectMocks
    private DefaultGoogleSheetValuesProvider provider;

    @Test
    void fetchValues_ShouldReturnValuesFromSheets() throws Exception {
        List<List<Object>> expected = List.of(List.of("nick", "user", 1));

        given(googleProperty.getKey()).willReturn("key.json");
        given(googleProperty.getId()).willReturn("sheetId");
        given(credentialsProvider.getCredentials("key.json")).willReturn(credentials);
        given(sheetsFactory.create(credentials)).willReturn(sheets);

        Sheets.Spreadsheets spreadsheets = BDDMockito.mock(Sheets.Spreadsheets.class);
        Sheets.Spreadsheets.Values values = BDDMockito.mock(Sheets.Spreadsheets.Values.class);
        Sheets.Spreadsheets.Values.Get get = BDDMockito.mock(Sheets.Spreadsheets.Values.Get.class);
        ValueRange valueRange = BDDMockito.mock(ValueRange.class);

        given(sheets.spreadsheets()).willReturn(spreadsheets);
        given(spreadsheets.values()).willReturn(values);
        given(values.get("sheetId", "호감도 순위표!B2:H2000")).willReturn(get);
        given(get.execute()).willReturn(valueRange);
        given(valueRange.getValues()).willReturn(expected);

        List<List<Object>> result = provider.fetchValues();

        then(result).isEqualTo(expected);
    }

    @Test
    void fetchValues_ShouldThrow_WhenSheetsFails() throws Exception {
        given(googleProperty.getKey()).willReturn("key.json");
        given(googleProperty.getId()).willReturn("sheetId");
        given(credentialsProvider.getCredentials("key.json")).willReturn(credentials);
        given(sheetsFactory.create(credentials)).willReturn(sheets);

        Sheets.Spreadsheets spreadsheets = BDDMockito.mock(Sheets.Spreadsheets.class);
        Sheets.Spreadsheets.Values values = BDDMockito.mock(Sheets.Spreadsheets.Values.class);
        Sheets.Spreadsheets.Values.Get get = BDDMockito.mock(Sheets.Spreadsheets.Values.Get.class);

        given(sheets.spreadsheets()).willReturn(spreadsheets);
        given(spreadsheets.values()).willReturn(values);
        given(values.get("sheetId", "호감도 순위표!B2:H2000")).willReturn(get);
        given(get.execute()).willThrow(new IOException("boom"));

        thenThrownBy(provider::fetchValues)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("boom");
    }
}
