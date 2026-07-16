package org.nowstart.nyangnyangbot.application.service.command;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface CommandVariableContributor {

    List<CommandVariableDefinition> definitions();

    Map<String, String> resolve(Set<String> requestedKeys, CommandVariableContext context);
}
