package org.nowstart.nyangnyangbot.adapter.out.external.google;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;

import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.api.services.sheets.v4.SheetsScopes;
import java.io.IOException;
import java.security.KeyPairGenerator;
import java.util.Base64;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.nowstart.nyangnyangbot.config.property.GoogleProperty;

class GoogleCredentialsTest {
    @Test
    void readsJsonWithoutAFile() throws Exception {
        var generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        String pem = "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getEncoder().encodeToString(generator.generateKeyPair().getPrivate().getEncoded())
                + "\n-----END PRIVATE KEY-----\n";
        String json = new JSONObject()
                .put("type", "service_account")
                .put("client_id", "123456")
                .put("client_email", "test@example.iam.gserviceaccount.com")
                .put("private_key_id", "test-key")
                .put("private_key", pem)
                .put("token_uri", "https://oauth2.googleapis.com/token").toString();
        var client = new GoogleSheetClientAdapter(new GoogleProperty("sheet-id", json));
        var credentials = (ServiceAccountCredentials) client.loadCredentials();
        then(credentials.getClientEmail()).isEqualTo("test@example.iam.gserviceaccount.com");
        then(credentials.getScopes()).containsExactly(SheetsScopes.SPREADSHEETS);
    }

    @Test
    void rejectsMissingInvalidAndNonServiceAccountCredentialsWithoutLeakingData() {
        for (String value : new String[] {null, " ", "SECRET-MARKER", "/run/secrets/google-sheet-key.json",
                "{\"type\":\"authorized_user\",\"refresh_token\":\"SECRET-MARKER\"}"}) {
            var client = new GoogleSheetClientAdapter(new GoogleProperty("sheet-id", value));
            thenThrownBy(client::loadCredentials).isInstanceOf(IOException.class)
                    .hasNoCause().hasMessageNotContaining("SECRET-MARKER");
        }
    }

    @Test
    void redactsPropertyString() {
        then(new GoogleProperty("sheet-id", "SECRET-MARKER").toString())
                .contains("[REDACTED]").doesNotContain("SECRET-MARKER");
    }
}
