package org.nowstart.nyangnyangbot.data.dto.sheet;

import java.util.List;

public record GoogleSheetDto(String nickName, String userId, Integer favorite) {

    public static GoogleSheetDto fromRow(List<Object> row) {
        if (row == null || row.size() < 3) {
            return null;
        }
        Integer favorite = parseFavorite(row.getLast());
        if (favorite == null) {
            return null;
        }
        return new GoogleSheetDto(
                (String) row.get(0),
                (String) row.get(1),
                favorite
        );
    }

    private static Integer parseFavorite(Object value) {
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
