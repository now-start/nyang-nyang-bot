package org.nowstart.nyangnyangbot.data.property;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "google.spreadsheet")
public record GoogleProperty(String id, String key) {
}
