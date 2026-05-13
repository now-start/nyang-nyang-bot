package org.nowstart.nyangnyangbot.adapter.out.external.google.response;

import java.util.List;
import org.nowstart.nyangnyangbot.application.port.out.google.GoogleSheetPort.GoogleSheetRow;

public record GoogleSheetRowResponse(List<Object> values) {

    public GoogleSheetRow toGoogleSheetRow() {
        if (values == null || values.size() < 3) {
            return null;
        }
        Integer favorite = parseFavorite(values.getLast());
        if (favorite == null) {
            return null;
        }
        return new GoogleSheetRow(
                (String) values.get(0),
                (String) values.get(1),
                favorite
        );
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
