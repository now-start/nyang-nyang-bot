package org.nowstart.nyangnyangbot.support;

import jakarta.validation.Validation;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.validation.beanvalidation.MethodValidationInterceptor;

public final class MethodValidationTestSupport {

    private MethodValidationTestSupport() {
    }

    public static <T> T validated(T target) {
        ProxyFactory proxyFactory = new ProxyFactory(target);
        proxyFactory.setProxyTargetClass(true);
        proxyFactory.addAdvice(new MethodValidationInterceptor(
                Validation.buildDefaultValidatorFactory().getValidator()
        ));
        @SuppressWarnings("unchecked")
        T proxy = (T) proxyFactory.getProxy();
        return proxy;
    }
}
