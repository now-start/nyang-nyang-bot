package org.nowstart.nyangnyangbot.application.validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UseCaseValidator {

    private final Validator validator;

    public <T> void validate(T value, String requiredMessage) {
        if (value == null) {
            throw new IllegalArgumentException(requiredMessage);
        }
        List<String> errors = errors(value);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join(", ", errors));
        }
    }

    public <T> List<String> errors(T value) {
        return validator.validate(value).stream()
                .sorted(Comparator
                        .comparing((ConstraintViolation<T> violation) -> violation.getPropertyPath().toString())
                        .thenComparing(ConstraintViolation::getMessage))
                .map(ConstraintViolation::getMessage)
                .distinct()
                .toList();
    }
}
