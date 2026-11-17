package org.nowstart.nyangnyangbot.config.property;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** key is decrypted service account JSON from Config Server, not a file path. */
@ConfigurationProperties(prefix = "google.spreadsheet")
public record GoogleProperty(String id, String key) {
    @Override
    public String toString() {
        return "GoogleProperty[id=" + id + ", key=[REDACTED]]";
    }
}
