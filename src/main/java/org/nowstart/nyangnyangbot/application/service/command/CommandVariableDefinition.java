package org.nowstart.nyangnyangbot.application.service.command;

public record CommandVariableDefinition(
        String key,
        String label,
        String description,
        String example
) {
}
