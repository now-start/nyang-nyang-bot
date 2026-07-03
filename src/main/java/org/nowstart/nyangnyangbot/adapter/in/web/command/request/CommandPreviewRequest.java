package org.nowstart.nyangnyangbot.adapter.in.web.command.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommandPreviewRequest(
        @NotBlank(message = "messageTemplate is required")
        @Size(max = 300, message = "messageTemplate length must be 300 or less")
        String messageTemplate,
        String nickname,
        String command,
        String args,
        String arg1,
        String arg2,
        Integer favorite
) {
}
