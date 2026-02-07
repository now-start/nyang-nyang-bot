package org.nowstart.nyangnyangbot.data.property;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "chzzk")
public record ChzzkProperty(
        String id,
        String password,
        String channelId,
        String clientId,
        String clientSecret,
        String redirectUri
) {
}
