package org.nowstart.nyangnyangbot.application.port.out.google;

import java.util.List;

public interface GoogleSheetPort {

    List<GoogleSheetRow> readFavoriteRows();

    record GoogleSheetRow(String nickName, String userId, Integer favorite) {
    }
}
