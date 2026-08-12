package org.nowstart.nyangnyangbot.application.exception;

/** 타이머 메시지 관리 요청을 업무 규칙상 처리할 수 없음을 나타낸다. */
public class TimerMessageManagementException extends RuntimeException {

    public TimerMessageManagementException(String message) {
        super(message);
    }
}
