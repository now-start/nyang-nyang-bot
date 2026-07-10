package org.nowstart.nyangnyangbot.adapter.out.external.google.response;

import java.util.List;
import java.util.Optional;
import org.nowstart.nyangnyangbot.application.port.out.google.GoogleSheetPort.GoogleSheetRow;

public record GoogleSheetRowResponse(List<Object> values) {

    public Optional<GoogleSheetRow> toGoogleSheetRow() {
        if (values == null || values.size() < 3) {
            return Optional.empty();
        }
        String nickName = parseText(values.get(0));
        String userId = parseText(values.get(1));
        Integer favorite = parseFavorite(values.getLast());
        if (nickName == null || userId == null || favorite == null) {
            return Optional.empty();
        }
        return Optional.of(new GoogleSheetRow(
                nickName,
                userId,
                favorite
        ));
    }

    private String parseText(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        return text.isEmpty() ? null : text;
    }

    private Integer parseFavorite(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        if (text.isEmpty()) {
            return null;
        }
        try {
            return Integer.valueOf(text);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
