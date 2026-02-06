package org.nowstart.nyangnyangbot.service;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;

import com.google.auth.oauth2.GoogleCredentials;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class DefaultGoogleCredentialsProviderTest {

    @Test
    void getCredentials_ShouldLoadAuthorizedUserCredentials() throws Exception {
        Path tempFile = Files.createTempFile("google-credentials", ".json");
        tempFile.toFile().deleteOnExit();
        String json = """
                {
                  "type": "authorized_user",
                  "client_id": "test-client",
                  "client_secret": "test-secret",
                  "refresh_token": "test-refresh"
                }
                """;
        Files.writeString(tempFile, json);

        DefaultGoogleCredentialsProvider provider = new DefaultGoogleCredentialsProvider();

        GoogleCredentials credentials = provider.getCredentials(tempFile.toString());

        then(credentials).isNotNull();
    }

    @Test
    void getCredentials_ShouldThrow_WhenFileMissing() {
        DefaultGoogleCredentialsProvider provider = new DefaultGoogleCredentialsProvider();

        thenThrownBy(() -> provider.getCredentials("missing-file.json"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}






