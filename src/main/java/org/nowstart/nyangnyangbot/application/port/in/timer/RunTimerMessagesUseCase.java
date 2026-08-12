package org.nowstart.nyangnyangbot.application.port.in.timer;

public interface RunTimerMessagesUseCase {

    /** 현재 시각에 전송할 타이머 메시지를 선점하고 전송한다. */
    void runDueTimerMessages();
}
