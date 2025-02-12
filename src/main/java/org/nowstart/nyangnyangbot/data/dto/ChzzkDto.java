package org.nowstart.nyangnyangbot.data.dto;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "chzzk")
public class ChzzkDto {

    private String channelId;
    private String id;
    private String password;
    private String clientId;
    private String clientSecret;
}
