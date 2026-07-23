package org.nowstart.nyangnyangbot.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.nowstart.nyangnyangbot.application.port.in.user.GetOAuthAccessTokenUseCase;
import org.nowstart.nyangnyangbot.application.port.out.user.OAuthCredentialPort.OAuthCredentialRecord;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationUtils;

@Configuration
@EnableFeignClients(basePackages = "org.nowstart.nyangnyangbot.adapter.out.external.chzzk")
public class FeignConfig {

    @Bean
    public RequestInterceptor requestInterceptor(
            ObjectProvider<GetOAuthAccessTokenUseCase> authorizationUseCaseProvider
    ) {
        return requestTemplate -> {
            if (isAuthorizedMethod(requestTemplate)) {
                OAuthCredentialRecord authentication = authorizationUseCaseProvider.getObject().getAccessToken();
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
