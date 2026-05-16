package org.nowstart.nyangnyangbot.application.service.donation;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.socket.emitter.Emitter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.application.port.in.roulette.ProcessRouletteDonationUseCase;
import org.nowstart.nyangnyangbot.application.port.out.donation.DonationPort;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort.DonationEventPayload;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DonationService implements Emitter.Listener {

    private final ObjectMapper objectMapper;
    private final DonationPort donationPort;
    private final ProcessRouletteDonationUseCase processRouletteDonationUseCase;

    @Override
    @SneakyThrows
    public void call(Object... objects) {
        DonationEventPayload donation = objectMapper.readValue((String) objects[0], DonationEventPayload.class);
        log.info("[ChzzkDonation] socket received: {}", donation);
        if (isBlank(donation.donationEventId())
                || !donationPort.existsByDonationEventId(donation.donationEventId())) {
            donationPort.save(donation, parseAmount(donation.payAmount()), toJson(donation.emojis()));
        }
        processRouletteDonationUseCase.processDonation(donation);
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
