package org.nowstart.nyangnyangbot.data.property;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "google.spreadsheet")
public class GoogleProperty {

    private String id;
    private String key;
}
