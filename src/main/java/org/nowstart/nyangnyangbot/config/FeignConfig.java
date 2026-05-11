package org.nowstart.nyangnyangbot.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.domain.model.AuthorizationAccount;
import org.nowstart.nyangnyangbot.application.service.authorization.AuthorizationService;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.annotation.AnnotationUtils;

@Configuration
@RequiredArgsConstructor(onConstructor_ = @Lazy)
@EnableFeignClients(basePackages = "org.nowstart.nyangnyangbot.adapter.out.external.chzzk")
public class FeignConfig {

    private final AuthorizationService authorizationService;

    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            if (isAuthorizedMethod(requestTemplate)) {
                AuthorizationAccount authentication = authorizationService.getAccessToken();
                if (authentication != null) {
                    String token = authentication.tokenType() + " " + authentication.accessToken();

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
