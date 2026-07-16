package org.nowstart.nyangnyangbot.adapter.out.validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.groups.Default;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.application.validation.outbound.ExternalResponseContractException;
import org.nowstart.nyangnyangbot.application.validation.outbound.OutboundRequestContractException;
import org.nowstart.nyangnyangbot.application.validation.outbound.OutboundResult;
import org.nowstart.nyangnyangbot.application.validation.outbound.PersistenceDataContractException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboundContractValidator {

    private final Validator validator;

    public <T> T request(String operation, T value) {
        return validate(
                value,
                violations -> new OutboundRequestContractException(message(operation, violations)),
                Default.class
        );
    }

    public <T> T externalResponse(String operation, T value) {
        return validate(
                value,
                violations -> new ExternalResponseContractException(message(operation, violations)),
                Default.class,
                OutboundResult.class
        );
    }

    public <T> T persistenceResult(String operation, T value) {
        return validate(
                value,
                violations -> new PersistenceDataContractException(message(operation, violations)),
                Default.class,
                OutboundResult.class
        );
    }

    private <T> T validate(
            T value,
            Function<Set<? extends ConstraintViolation<?>>, RuntimeException> exceptionFactory,
            Class<?>... groups
    ) {
        if (value == null) {
            throw exceptionFactory.apply(Set.of());
        }
        Set<ConstraintViolation<T>> violations = validator.validate(value, groups);
        if (!violations.isEmpty()) {
            throw exceptionFactory.apply(violations);
        }
        return value;
    }

    private String message(String operation, Set<? extends ConstraintViolation<?>> violations) {
        String details = violations.isEmpty()
                ? "value is required"
                : violations.stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .sorted()
                .collect(Collectors.joining(", "));
        return "Outbound contract violation: operation=%s, violations=%s".formatted(operation, details);
    }
}
