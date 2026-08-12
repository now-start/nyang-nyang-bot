package org.nowstart.nyangnyangbot.application.exception;

/** 포인트 조정 요청을 업무 규칙상 처리할 수 없음을 나타낸다. */
public class PointAdjustmentException extends RuntimeException {

    public PointAdjustmentException(String message) {
        super(message);
    }

    public PointAdjustmentException(String message, Throwable cause) {
        super(message, cause);
    }
}
