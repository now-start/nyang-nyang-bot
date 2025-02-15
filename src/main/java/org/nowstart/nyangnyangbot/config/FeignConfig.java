package org.nowstart.nyangnyangbot.config;

import feign.RequestInterceptor;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.data.entity.AuthorizationEntity;
import org.nowstart.nyangnyangbot.service.AuthorizationService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@EnableFeignClients(basePackages = "org.nowstart.nyangnyangbot.repository", defaultConfiguration = FeignConfig.class)
public class FeignConfig {

    private final ObjectProvider<AuthorizationService> authorizationServiceProvider;

    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            if (requestTemplate.path().contains("/open/v1")) {
                AuthorizationService authorizationService = authorizationServiceProvider.getIfAvailable();
                if (authorizationService != null) {
                    AuthorizationEntity authentication = authorizationService.getAccessToken();
                    String token = authentication.getTokenType() + " " + authentication.getAccessToken();

                    requestTemplate.header("Content-Type", "application/json");
                    requestTemplate.header("Authorization", token);
                }
            }
        };
    }
}
