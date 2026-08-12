package org.nowstart.nyangnyangbot.adapter.in.web.error;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ElementKind;
import java.util.stream.StreamSupport;
import org.nowstart.nyangnyangbot.application.exception.ExternalSystemException;

/** MVC 컨트롤러가 내부 계약 위반과 외부 시스템 장애를 입력 오류로 숨기지 않도록 구분한다. */
public final class WebExceptionSupport {

    private WebExceptionSupport() {
    }

    public static boolean isReturnValueViolation(ConstraintViolationException exception) {
        return exception.getConstraintViolations().stream()
                .flatMap(violation -> StreamSupport.stream(violation.getPropertyPath().spliterator(), false))
                .anyMatch(node -> node.getKind() == ElementKind.RETURN_VALUE);
    }

    public static void rethrowIfInternalFailure(RuntimeException exception) {
        if (exception instanceof ExternalSystemException) {
            throw exception;
        }
        if (exception instanceof ConstraintViolationException violation && isReturnValueViolation(violation)) {
            throw exception;
        }
    }
}
