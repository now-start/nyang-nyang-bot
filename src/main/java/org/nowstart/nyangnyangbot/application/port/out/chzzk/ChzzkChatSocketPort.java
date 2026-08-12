package org.nowstart.nyangnyangbot.application.port.out.chzzk;

public interface ChzzkChatSocketPort {

    /** 세션의 채팅 소켓을 열고 콜백에 연결 시도 식별자를 함께 전달한다. */
    void connect(String sessionUrl, long connectionAttemptId);
}
