package org.nowstart.nyangnyangbot.adapter.in.web.command.request;

import static org.assertj.core.api.BDDAssertions.then;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

class CommandRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void createRequest_ShouldLeaveConditionalFieldsToApplicationValidation() {
        // 준비
        CommandCreateRequest request = new CommandCreateRequest(
                "TEXT",
                "!공지",
                null,
                "{nickname}",
                0,
                0,
                true,
                "",
                0
        );

        // 실행 및 검증
        then(validator.validate(request)).isEmpty();
    }

    @Test
    void updateRequest_ShouldAllowBlankOptionalFieldsAtDtoLevel() {
        // 준비
        CommandUpdateRequest request = new CommandUpdateRequest(
                "",
                null,
                null,
                null,
                0,
                0,
                null,
                "",
                0
        );

        // 실행 및 검증
        then(validator.validate(request)).isEmpty();
    }

    @Test
    void previewRequest_ShouldRequireMessageTemplate() {
        // 준비
        CommandPreviewRequest request = new CommandPreviewRequest(
                "",
                "치즈냥",
                "!점수",
                null,
                null,
                null,
                null
        );

        // 실행 및 검증
        then(validator.validate(request)).isNotEmpty();
    }
}
