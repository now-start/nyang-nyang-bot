package org.nowstart.nyangnyangbot.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.data.entity.AuthorizationEntity;
import org.nowstart.nyangnyangbot.service.AuthorizationService;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.annotation.AnnotationUtils;

@Configuration
@RequiredArgsConstructor(onConstructor_ = @Lazy)
@EnableFeignClients(basePackages = "org.nowstart.nyangnyangbot.repository")
public class FeignConfig {

    private final AuthorizationService authorizationService;

    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            if (isAuthorizedMethod(requestTemplate)) {
                AuthorizationEntity authentication = authorizationService.getAccessToken();
                if (authentication != null) {
                    String token = authentication.getTokenType() + " " + authentication.getAccessToken();

                    requestTemplate.header("Content-Type", "application/json");
                    requestTemplate.header("Authorization", token);
                }
            }
        };
    }

    private boolean isAuthorizedMethod(RequestTemplate requestTemplate) {
        return AnnotationUtils.findAnnotation(requestTemplate.methodMetadata().method(), Authorization.class) != null;
    }
}
