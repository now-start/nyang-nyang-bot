package org.nowstart.nyangnyangbot.data.property;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "chzzk")
public class ChzzkProperty {

    private String id;
    private String password;
    private String channelId;
    private String clientId;
    private String clientSecret;
    private String redirectUri;
}
