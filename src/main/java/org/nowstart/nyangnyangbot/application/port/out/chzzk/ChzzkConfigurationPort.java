package org.nowstart.nyangnyangbot.application.port.out.chzzk;

public interface ChzzkConfigurationPort {

    /** 설정된 CHZZK 채널 식별자를 반환한다. */
    String channelId();

    /** 설정된 CHZZK 클라이언트 식별자를 반환한다. */
    String clientId();

    /** 설정된 CHZZK 클라이언트 시크릿을 반환한다. */
    String clientSecret();
}
