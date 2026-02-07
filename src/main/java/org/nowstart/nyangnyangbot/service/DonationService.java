package org.nowstart.nyangnyangbot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.socket.emitter.Emitter;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.data.dto.chzzk.DonationDto;
import org.nowstart.nyangnyangbot.data.entity.ChannelEntity;
import org.nowstart.nyangnyangbot.data.entity.DonationEntity;
import org.nowstart.nyangnyangbot.repository.DonationRepository;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class DonationService implements Emitter.Listener {

    private final ObjectMapper objectMapper;
    private final DonationRepository donationRepository;
    private final ChannelService channelService;

    @Override
    @SneakyThrows
    public void call(Object... objects) {
        DonationDto donationDto = objectMapper.readValue((String) objects[0], DonationDto.class);
        log.info("[ChzzkDonation] socket received: {}", donationDto);
        ChannelEntity channel = channelService.getOrCreate(donationDto.channelId(), null);
        ChannelEntity donatorChannel = channelService.getOrCreate(donationDto.donatorChannelId(), donationDto.donatorNickname());
        if (channel == null) {
            return;
        }
        donationRepository.save(DonationEntity.builder()
                .donationType(donationDto.donationType())
                .channel(channel)
                .donatorChannel(donatorChannel)
                .payAmount(parseAmount(donationDto.payAmount()))
                .donationText(donationDto.donationText())
                .emojisJson(toJson(donationDto.emojis()))
                .build());
    }

    @SneakyThrows
    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        return objectMapper.writeValueAsString(value);
    }

    private Long parseAmount(String amount) {
        if (amount == null || amount.isBlank()) {
            return 0L;
        }
        String digits = amount.replaceAll("\\D", "");
        if (digits.isBlank()) {
            return 0L;
        }
        try {
            return Long.parseLong(digits);
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }
}
