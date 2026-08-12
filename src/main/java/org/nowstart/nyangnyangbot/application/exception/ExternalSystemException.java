package org.nowstart.nyangnyangbot.application.exception;

/** 외부 시스템이 요청을 정상적으로 처리하지 못했음을 나타낸다. */
public class ExternalSystemException extends RuntimeException {

    public ExternalSystemException(String message) {
        super(message);
    }

    public ExternalSystemException(String message, Throwable cause) {
        super(message, cause);
    }
}
