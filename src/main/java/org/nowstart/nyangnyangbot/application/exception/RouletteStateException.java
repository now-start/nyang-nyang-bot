package org.nowstart.nyangnyangbot.application.exception;

/** 룰렛 설정이 요청한 관리 작업을 허용하지 않는 상태임을 나타낸다. */
public class RouletteStateException extends RouletteManagementException {

    public RouletteStateException(String message) {
        super(message);
    }

    public RouletteStateException(String message, Throwable cause) {
        super(message, cause);
    }
}
