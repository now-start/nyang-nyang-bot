package org.nowstart.nyangnyangbot.application.port.in.timer;

public interface RecordTimerChatUseCase {

    /** 타이머 메시지 전송 조건을 판단하는 채팅 활동 횟수를 증가시킨다. */
    void recordChatActivity();
}
