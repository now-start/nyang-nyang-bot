package org.nowstart.nyangnyangbot.application.port.in.google;

public interface SyncGoogleSheetUseCase {

    /** 애플리케이션의 포인트 잔액을 설정된 Google Sheet와 일치시킨다. */
    void synchronizePoints();
}
