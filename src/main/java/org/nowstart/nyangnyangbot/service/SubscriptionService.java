package org.nowstart.nyangnyangbot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.socket.emitter.Emitter;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.data.dto.chzzk.SubscriptionDto;
import org.nowstart.nyangnyangbot.data.entity.SubscriptionEntity;
import org.nowstart.nyangnyangbot.repository.SubscriptionRepository;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class SubscriptionService implements Emitter.Listener {

    private final ObjectMapper objectMapper;
    private final SubscriptionRepository subscriptionRepository;

    @Override
    @SneakyThrows
    public void call(Object... objects) {
        SubscriptionDto subscriptionDto = objectMapper.readValue((String) objects[0], SubscriptionDto.class);
        log.info("[ChzzkSubscription] socket received: {}", subscriptionDto);
        subscriptionRepository.save(SubscriptionEntity.builder()
                .channelId(subscriptionDto.channelId())
                .subscriberChannelId(subscriptionDto.subscriberChannelId())
                .subscriberNickname(subscriptionDto.subscriberNickname())
                .tierNo(subscriptionDto.tierNo())
                .tierName(subscriptionDto.tierName())
                .month(subscriptionDto.month())
                .build());
    }
}
