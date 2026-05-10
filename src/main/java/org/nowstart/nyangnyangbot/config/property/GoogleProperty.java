package org.nowstart.nyangnyangbot.config.property;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "google.spreadsheet")
public record GoogleProperty(String id, String key) {
}
