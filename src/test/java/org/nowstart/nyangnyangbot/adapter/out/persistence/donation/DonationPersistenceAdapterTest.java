package org.nowstart.nyangnyangbot.adapter.out.persistence.donation;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.adapter.out.persistence.donation.entity.Donation;
import org.nowstart.nyangnyangbot.adapter.out.persistence.donation.repository.DonationRepository;
import org.nowstart.nyangnyangbot.application.port.out.donation.DonationPort.SaveDonationCommand;

@ExtendWith(MockitoExtension.class)
class DonationPersistenceAdapterTest {

    @Mock
    private DonationRepository donationRepository;

    @Test
    void save_ShouldStoreBlankDonationEventIdAsNull() {
        // 준비
        DonationPersistenceAdapter adapter = new DonationPersistenceAdapter(donationRepository, new ObjectMapper());
        ArgumentCaptor<Donation> captor = ArgumentCaptor.forClass(Donation.class);
        SaveDonationCommand donation = new SaveDonationCommand(
                " ",
                "CHAT",
                "channel-1",
                "user-1",
                "치즈냥",
                1_000L,
                "후원",
                Map.of()
        );

        // 실행
        adapter.save(donation);

        // 검증
        verify(donationRepository).save(captor.capture());
        then(captor.getValue().getDonationEventId()).isNull();
        then(captor.getValue().getEmojisJson()).isEqualTo("{}");
    }
}
