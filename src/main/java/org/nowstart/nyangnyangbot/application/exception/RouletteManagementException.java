package org.nowstart.nyangnyangbot.application.exception;

/** 룰렛 관리 요청을 현재 설정이나 업무 규칙상 처리할 수 없음을 나타낸다. */
public class RouletteManagementException extends RuntimeException {

    public RouletteManagementException(String message) {
        super(message);
    }

    public RouletteManagementException(String message, Throwable cause) {
        super(message, cause);
    }
}
