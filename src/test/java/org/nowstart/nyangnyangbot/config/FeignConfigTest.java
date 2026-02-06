package org.nowstart.nyangnyangbot.config;

import static org.assertj.core.api.BDDAssertions.then;

import feign.MethodMetadata;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.service.ChzzkOAuth2TokenService;

@ExtendWith(MockitoExtension.class)
class FeignConfigTest {

    @Mock
    private ChzzkOAuth2TokenService tokenService;
    @InjectMocks
    private FeignConfig feignConfig;

    private static RequestTemplate templateFor(Method method) {
        RequestTemplate template = new RequestTemplate();
        MethodMetadata metadata = newMethodMetadata();
        setFieldByType(metadata, Method.class, method);
        setFieldByType(template, MethodMetadata.class, metadata);
        return template;
    }

    private static MethodMetadata newMethodMetadata() {
        try {
            java.lang.reflect.Constructor<MethodMetadata> constructor = MethodMetadata.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static void setFieldByType(Object target, Class<?> fieldType, Object value) {
        Field field = Arrays.stream(target.getClass().getDeclaredFields())
                .filter(candidate -> candidate.getType().equals(fieldType))
                .findFirst()
                .orElseThrow();
        field.setAccessible(true);
        try {
            field.set(target, value);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void requestInterceptor_ShouldAddHeaders_WhenAuthorizedAndTokenPresent() throws Exception {
        BDDMockito.given(tokenService.getAccessTokenValue()).willReturn("token123");

        RequestInterceptor interceptor = feignConfig.requestInterceptor();
        RequestTemplate template = templateFor(TestClient.class.getMethod("authorized"));

        interceptor.apply(template);

        then(template.headers().get("Authorization")).containsExactly("Bearer token123");
        then(template.headers().get("Content-Type")).containsExactly("application/json");
    }

    @Test
    void requestInterceptor_ShouldSkip_WhenNotAuthorizedMethod() throws Exception {
        RequestInterceptor interceptor = feignConfig.requestInterceptor();
        RequestTemplate template = templateFor(TestClient.class.getMethod("plain"));

        interceptor.apply(template);

        BDDMockito.then(tokenService).shouldHaveNoInteractions();
        then(template.headers().get("Authorization")).isNull();
    }

    @Test
    void requestInterceptor_ShouldPropagate_WhenTokenMissing() throws Exception {
        BDDMockito.given(tokenService.getAccessTokenValue()).willThrow(new IllegalStateException("missing"));

        RequestInterceptor interceptor = feignConfig.requestInterceptor();
        RequestTemplate template = templateFor(TestClient.class.getMethod("authorized"));

        org.assertj.core.api.BDDAssertions.thenThrownBy(() -> interceptor.apply(template))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("missing");

        BDDMockito.then(tokenService).should().getAccessTokenValue();
        then(template.headers().get("Authorization")).isNull();
        then(template.headers().get("Content-Type")).isNull();
    }

    interface TestClient {
        @Authorization
        void authorized();

        void plain();
    }
}






