package org.nowstart.nyangnyangbot.application.exception;

/** 명령어 관리 요청을 업무 규칙상 처리할 수 없음을 나타낸다. */
public class CommandManagementException extends RuntimeException {

    public CommandManagementException(String message) {
        super(message);
    }
}
