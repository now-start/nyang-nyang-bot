package org.nowstart.nyangnyangbot.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.socket.emitter.Emitter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.application.gateway.out.donation.DonationPort;
import org.nowstart.nyangnyangbot.application.chzzk.dto.DonationDto;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DonationService implements Emitter.Listener {

    private final ObjectMapper objectMapper;
    private final DonationPort donationPort;
    private final RouletteService rouletteService;

    @Override
    @SneakyThrows
    public void call(Object... objects) {
        DonationDto donationDto = objectMapper.readValue((String) objects[0], DonationDto.class);
        log.info("[ChzzkDonation] socket received: {}", donationDto);
        if (isBlank(donationDto.donationEventId())
                || !donationPort.existsByDonationEventId(donationDto.donationEventId())) {
            donationPort.save(donationDto, parseAmount(donationDto.payAmount()), toJson(donationDto.emojis()));
        }
        rouletteService.processDonation(donationDto);
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

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
