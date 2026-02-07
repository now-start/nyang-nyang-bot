package org.nowstart.nyangnyangbot.service;

import io.micrometer.common.util.StringUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.data.entity.ChannelEntity;
import org.nowstart.nyangnyangbot.data.property.ChzzkProperty;
import org.nowstart.nyangnyangbot.repository.ChannelRepository;
import org.springframework.stereotype.Service;

@Service
@Transactional
@RequiredArgsConstructor
public class ChannelService {

    private final ChannelRepository channelRepository;
    private final ChzzkProperty chzzkProperty;

    public ChannelEntity getDefaultChannel() {
        return getOrCreate(chzzkProperty.channelId(), null);
    }

    public ChannelEntity getOrCreate(String channelId, String name) {
        if (StringUtils.isBlank(channelId)) {
            return null;
        }
        ChannelEntity channel = channelRepository.findById(channelId)
                .orElseGet(() -> channelRepository.save(ChannelEntity.builder()
                        .id(channelId)
                        .name(name)
                        .build()));

        if (!StringUtils.isBlank(name) && !name.equals(channel.getName())) {
            channel.setName(name);
        }
        return channel;
    }
}
