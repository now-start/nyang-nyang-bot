package org.nowstart.nyangnyangbot.application.service.command;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.stereotype.Component;

@Component
public class CommandVariableRegistry {

    private final Map<String, RegisteredVariable> variables;
    private final List<CommandVariableDefinition> publicDefinitions;

    public CommandVariableRegistry(List<CommandVariableContributor> contributors) {
        Map<String, RegisteredVariable> registered = new LinkedHashMap<>();
        for (CommandVariableContributor contributor : contributors) {
            for (CommandVariableDefinition definition : contributor.definitions()) {
                validateDefinition(definition);
                RegisteredVariable previous = registered.putIfAbsent(
                        definition.key(),
                        new RegisteredVariable(definition, contributor)
                );
                if (previous != null) {
                    throw new IllegalStateException("duplicate command variable: " + definition.key());
                }
            }
        }
        this.variables = Map.copyOf(registered);
        this.publicDefinitions = registered.values().stream()
                .map(RegisteredVariable::definition)
                .sorted(Comparator.comparing(CommandVariableDefinition::key))
                .toList();
    }

    public List<CommandVariableDefinition> definitions() {
        return publicDefinitions;
    }

    public Set<String> unknownVariables(Collection<String> requestedKeys) {
        Set<String> unknown = new TreeSet<>();
        for (String key : requestedKeys) {
            if (!variables.containsKey(key)) {
                unknown.add(key);
            }
        }
        return Collections.unmodifiableSet(unknown);
    }

    public Map<String, String> resolve(Set<String> requestedKeys, CommandVariableContext context) {
        Set<String> unknown = unknownVariables(requestedKeys);
        if (!unknown.isEmpty()) {
            throw new IllegalArgumentException("unknown template variables: " + String.join(", ", unknown));
        }

        Map<CommandVariableContributor, Set<String>> keysByContributor = new LinkedHashMap<>();
        for (String key : requestedKeys) {
            CommandVariableContributor contributor = variables.get(key).contributor();
            keysByContributor.computeIfAbsent(contributor, ignored -> new LinkedHashSet<>()).add(key);
        }

        Map<String, String> resolved = new LinkedHashMap<>();
        for (Map.Entry<CommandVariableContributor, Set<String>> entry : keysByContributor.entrySet()) {
            Map<String, String> contributed = entry.getKey().resolve(Set.copyOf(entry.getValue()), context);
            for (String key : entry.getValue()) {
                if (!contributed.containsKey(key)) {
                    throw new IllegalStateException("command variable was not resolved: " + key);
                }
                String value = contributed.get(key);
                if (value == null) {
                    throw new IllegalStateException("command variable resolved to null: " + key);
                }
                resolved.put(key, value);
            }
        }
        return Map.copyOf(resolved);
    }

    public Map<String, String> sampleValues(Set<String> requestedKeys) {
        Set<String> unknown = unknownVariables(requestedKeys);
        if (!unknown.isEmpty()) {
            throw new IllegalArgumentException("unknown template variables: " + String.join(", ", unknown));
        }
        Map<String, String> samples = new LinkedHashMap<>();
        for (String key : requestedKeys) {
            samples.put(key, variables.get(key).definition().example());
        }
        return Map.copyOf(samples);
    }

    private void validateDefinition(CommandVariableDefinition definition) {
        List<String> missing = new ArrayList<>();
        if (definition == null) {
            throw new IllegalStateException("command variable definition is required");
        }
        if (!CommandTemplateRenderer.isValidVariableName(definition.key())) {
            throw new IllegalStateException("invalid command variable key: " + definition.key());
        }
        if (definition.label() == null || definition.label().isBlank()) {
            missing.add("label");
        }
        if (definition.description() == null || definition.description().isBlank()) {
            missing.add("description");
        }
        if (definition.example() == null) {
            missing.add("example");
        }
        if (!missing.isEmpty()) {
            throw new IllegalStateException(
                    "invalid command variable metadata for " + definition.key() + ": " + String.join(", ", missing)
            );
        }
    }

    private record RegisteredVariable(
            CommandVariableDefinition definition,
            CommandVariableContributor contributor
    ) {
    }
}
