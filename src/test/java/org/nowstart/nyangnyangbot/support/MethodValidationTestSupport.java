package org.nowstart.nyangnyangbot.support;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.validation.beanvalidation.MethodValidationInterceptor;

public final class MethodValidationTestSupport {

    private MethodValidationTestSupport() {
    }

    public static <T> T validated(T target, Class<T> useCaseType) {
        ProxyFactory proxyFactory = new ProxyFactory(target);
        proxyFactory.setInterfaces(useCaseType);
        proxyFactory.addAdvice(new MethodValidationInterceptor());
        return useCaseType.cast(proxyFactory.getProxy());
    }
}
