package org.nowstart.nyangnyangbot.data.property;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "google.spreadsheet")
public class GoogleProperty {

    private String id;
    private String key;
}
