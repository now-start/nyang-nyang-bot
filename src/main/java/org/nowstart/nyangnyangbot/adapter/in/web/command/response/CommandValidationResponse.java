package org.nowstart.nyangnyangbot.adapter.in.web.command.response;

import java.util.List;
import org.nowstart.nyangnyangbot.application.port.in.command.ManageCommandUseCase.ValidationResult;

public record CommandValidationResponse(boolean valid, List<String> errors) {

    public static CommandValidationResponse from(ValidationResult result) {
        return new CommandValidationResponse(result.valid(), result.errors());
    }
}
