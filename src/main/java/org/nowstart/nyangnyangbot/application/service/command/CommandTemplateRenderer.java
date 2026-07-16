package org.nowstart.nyangnyangbot.application.service.command;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class CommandTemplateRenderer {

    private static final int MAX_RENDERED_LENGTH = 300;
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{([^{}]*)}");
    private static final Pattern VARIABLE_NAME_PATTERN = Pattern.compile(
            "[A-Za-z][A-Za-z0-9]*(?:\\.[A-Za-z][A-Za-z0-9]*)*"
    );
    private static final Pattern VARIABLE_PATTERN = Pattern.compile(
            "\\{([A-Za-z][A-Za-z0-9]*(?:\\.[A-Za-z][A-Za-z0-9]*)*)}"
    );

    public static boolean isValidVariableName(String value) {
        return value != null && VARIABLE_NAME_PATTERN.matcher(value).matches();
    }

    public Set<String> variables(String template) {
        if (template == null || template.isBlank()) {
            return Set.of();
        }
        Set<String> variables = new TreeSet<>();
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        while (matcher.find()) {
            String variable = matcher.group(1);
            if (isValidVariableName(variable)) {
                variables.add(variable);
            }
        }
        return Collections.unmodifiableSet(variables);
    }

    public Set<String> malformedVariables(String template) {
        if (template == null || template.isBlank()) {
            return Set.of();
        }
        Set<String> malformed = new TreeSet<>();
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        StringBuilder withoutPlaceholders = new StringBuilder();
        while (matcher.find()) {
            String variable = matcher.group(1);
            if (!isValidVariableName(variable)) {
                malformed.add(variable.isBlank() ? matcher.group() : variable);
            }
            matcher.appendReplacement(withoutPlaceholders, "");
        }
        matcher.appendTail(withoutPlaceholders);
        if (withoutPlaceholders.indexOf("{") >= 0 || withoutPlaceholders.indexOf("}") >= 0) {
            malformed.add("unmatched braces");
        }
        return Collections.unmodifiableSet(malformed);
    }

    public String render(String template, Map<String, String> values) {
        Set<String> malformed = malformedVariables(template);
        if (!malformed.isEmpty()) {
            throw new IllegalArgumentException("malformed template variables: " + String.join(", ", malformed));
        }
        Set<String> missing = new TreeSet<>(variables(template));
        missing.removeAll(values.keySet());
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException("unresolved template variables: " + String.join(", ", missing));
        }

        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        StringBuilder rendered = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(
                    rendered,
                    Matcher.quoteReplacement(safe(values.get(matcher.group(1))))
            );
        }
        matcher.appendTail(rendered);
        return limit(rendered.toString());
    }

    private String safe(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("\\p{Cntrl}", " ")
                .replaceAll("\\s+", " ")
                .replaceAll("(?i)https://", "https[:]//")
                .replaceAll("(?i)http://", "http[:]//")
                .replaceAll("(?i)www\\.", "www[.]")
                .replace("@", "[at]")
                .trim();
    }

    private String limit(String value) {
        int codePointCount = value.codePointCount(0, value.length());
        if (codePointCount <= MAX_RENDERED_LENGTH) {
            return value;
        }
        return value.substring(0, value.offsetByCodePoints(0, MAX_RENDERED_LENGTH));
    }
}
