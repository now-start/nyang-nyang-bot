package org.nowstart.nyangnyangbot.application.port.in.authorization;

public interface LoginWithChzzkUseCase {

    Result login(String code, String state);

    record Result(
            String channelId,
            boolean admin
    ) {
    }
}
