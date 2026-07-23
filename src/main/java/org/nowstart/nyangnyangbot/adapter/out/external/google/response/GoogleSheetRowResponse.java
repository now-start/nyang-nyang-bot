package org.nowstart.nyangnyangbot.adapter.out.external.google.response;

import java.util.List;
import java.util.Optional;
import org.nowstart.nyangnyangbot.application.port.out.google.GoogleSheetPort.GoogleSheetRow;

public record GoogleSheetRowResponse(List<Object> values) {

    public Optional<GoogleSheetRow> toGoogleSheetRow() {
        if (values == null || values.size() < 3) {
            return Optional.empty();
        }
        String displayName = parseText(values.get(0));
        String userId = parseText(values.get(1));
        Long point = parsePoint(values.getLast());
        if (displayName == null || userId == null || point == null) {
            return Optional.empty();
        }
        return Optional.of(new GoogleSheetRow(
                displayName,
                userId,
                point
        ));
    }

    private String parseText(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        return text.isEmpty() ? null : text;
    }

    private Long parsePoint(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        if (text.isEmpty()) {
            return null;
        }
        try {
            return Long.valueOf(text);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
