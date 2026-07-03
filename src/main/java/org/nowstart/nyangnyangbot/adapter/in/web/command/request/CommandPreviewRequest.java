package org.nowstart.nyangnyangbot.adapter.in.web.command.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommandPreviewRequest(
        @NotBlank
        @Size(max = 300)
        String messageTemplate,
        String nickname,
        String command,
        String args,
        String arg1,
        String arg2,
        Integer favorite
) {
}
