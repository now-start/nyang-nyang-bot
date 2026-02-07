package org.nowstart.nyangnyangbot.data.dto.sheet;

import java.util.List;

public record GoogleSheetDto(String nickName, String userId, int favorite) {

    public static GoogleSheetDto fromRow(List<Object> row) {
        if (row == null || row.size() < 3) {
            return null;
        }
        return new GoogleSheetDto(
                (String) row.get(0),
                (String) row.get(1),
                Integer.parseInt(row.getLast().toString())
        );
    }
}
