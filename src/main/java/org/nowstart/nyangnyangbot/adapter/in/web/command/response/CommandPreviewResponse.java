package org.nowstart.nyangnyangbot.adapter.in.web.command.response;

import org.nowstart.nyangnyangbot.application.port.in.command.ManageCommandUseCase.PreviewResult;

public record CommandPreviewResponse(String message) {

    public static CommandPreviewResponse from(PreviewResult result) {
        return new CommandPreviewResponse(result.message());
    }
}
