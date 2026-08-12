package org.nowstart.nyangnyangbot.application.exception;

/** 생존 확인 보상 요청을 현재 수집 상태나 대상 조건상 처리할 수 없음을 나타낸다. */
public class PresenceRewardException extends RuntimeException {

    public PresenceRewardException(String message) {
        super(message);
    }
}
