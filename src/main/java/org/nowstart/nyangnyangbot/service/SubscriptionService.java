package org.nowstart.nyangnyangbot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.socket.emitter.Emitter;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.application.port.out.subscription.SubscriptionPort;
import org.nowstart.nyangnyangbot.data.dto.chzzk.SubscriptionDto;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class SubscriptionService implements Emitter.Listener {

    private final ObjectMapper objectMapper;
    private final SubscriptionPort subscriptionPort;

    @Override
    @SneakyThrows
    public void call(Object... objects) {
        SubscriptionDto subscriptionDto = objectMapper.readValue((String) objects[0], SubscriptionDto.class);
        log.info("[ChzzkSubscription] socket received: {}", subscriptionDto);
        subscriptionPort.save(subscriptionDto);
    }
}
