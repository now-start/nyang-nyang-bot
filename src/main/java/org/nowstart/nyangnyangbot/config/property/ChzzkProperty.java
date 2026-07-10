package org.nowstart.nyangnyangbot.config.property;

import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkConfigurationPort;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "chzzk")
public record ChzzkProperty(
        String channelId,
        String clientId,
        String clientSecret,
        String redirectUri
) implements ChzzkConfigurationPort {
}
