package org.nowstart.nyangnyangbot.application.exception;

/** 오버레이 표시 요청을 현재 작업 상태나 접근 조건상 처리할 수 없음을 나타낸다. */
public class OverlayDisplayException extends RuntimeException {

    public OverlayDisplayException(String message) {
        super(message);
    }

    public OverlayDisplayException(String message, Throwable cause) {
        super(message, cause);
    }
}
