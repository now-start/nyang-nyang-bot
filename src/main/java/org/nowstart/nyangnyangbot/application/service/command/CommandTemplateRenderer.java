package org.nowstart.nyangnyangbot.application.service.command;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class CommandTemplateRenderer {

    private static final int MAX_VARIABLE_VALUE_LENGTH = 60;
    private static final int MAX_RENDERED_LENGTH = 300;
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{([^{}]*)}");
    private static final Pattern VARIABLE_NAME_PATTERN = Pattern.compile("[A-Za-z][A-Za-z0-9]*");
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{([A-Za-z][A-Za-z0-9]*)}");
    private static final Set<String> ALLOWED_VARIABLES = Set.of(
            "nickname",
            "command",
            "args",
            "arg1",
            "arg2",
            "favorite",
            "date",
            "time",
            "datetime"
    );
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static Set<String> unknownVariables(String template) {
        if (template == null || template.isBlank()) {
            return Set.of();
        }
        Set<String> unknown = new TreeSet<>();
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        while (matcher.find()) {
            String variable = matcher.group(1);
            if (!VARIABLE_NAME_PATTERN.matcher(variable).matches() || !ALLOWED_VARIABLES.contains(variable)) {
                unknown.add(variable.isBlank() ? matcher.group() : variable);
            }
        }
        return unknown;
    }

    public boolean usesVariable(String template, String variable) {
        return template != null && template.contains("{" + variable + "}");
    }

    public String render(String template, TemplateContext context) {
        Set<String> unknown = unknownVariables(template);
        if (!unknown.isEmpty()) {
            throw new IllegalArgumentException("unknown template variables: " + String.join(", ", unknown));
        }
        Map<String, String> values = Map.of(
                "nickname", safe(context.nickname()),
                "command", safe(context.command()),
                "args", safe(context.args()),
                "arg1", safe(context.arg1()),
                "arg2", safe(context.arg2()),
                "favorite", String.valueOf(context.favorite() == null ? 0 : context.favorite()),
                "date", context.now().format(DATE_FORMAT),
                "time", context.now().format(TIME_FORMAT),
                "datetime", context.now().format(DATE_TIME_FORMAT)
        );
        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        StringBuilder rendered = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(rendered, Matcher.quoteReplacement(values.get(matcher.group(1))));
        }
        matcher.appendTail(rendered);
        return limit(rendered.toString(), MAX_RENDERED_LENGTH);
    }

    private String safe(String value) {
        if (value == null) {
            return "";
        }
        String sanitized = value.replaceAll("\\p{Cntrl}", " ")
                .replaceAll("\\s+", " ")
                .replaceAll("(?i)https://", "https[:]//")
                .replaceAll("(?i)http://", "http[:]//")
                .replaceAll("(?i)www\\.", "www[.]")
                .replace("@", "[at]")
                .trim();
        return limit(sanitized, MAX_VARIABLE_VALUE_LENGTH);
    }

    private String limit(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    public record TemplateContext(
            String nickname,
            String command,
            String args,
            String arg1,
            String arg2,
            Integer favorite,
            LocalDateTime now
    ) {
    }
}
