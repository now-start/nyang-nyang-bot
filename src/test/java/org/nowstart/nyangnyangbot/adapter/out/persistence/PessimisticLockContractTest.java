package org.nowstart.nyangnyangbot.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.LockModeType;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.nowstart.nyangnyangbot.adapter.out.persistence.command.repository.CommandRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.repository.RouletteConfigRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.timer.repository.TimerMessageRepository;
import org.nowstart.nyangnyangbot.domain.type.RouletteConfigStatus;
import org.springframework.data.jpa.repository.Lock;

class PessimisticLockContractTest {

    @Test
    void mutableAdminReadsUsePessimisticWriteLocks() throws NoSuchMethodException {
        assertPessimisticWrite(CommandRepository.class.getMethod("findByIdForUpdate", Long.class));
        assertPessimisticWrite(TimerMessageRepository.class.getMethod("findByIdForUpdate", Long.class));
        assertPessimisticWrite(RouletteConfigRepository.class.getMethod(
                "findByStatusForUpdate", RouletteConfigStatus.class));
    }

    private void assertPessimisticWrite(Method method) {
        assertThat(method.getAnnotation(Lock.class))
                .isNotNull()
                .extracting(Lock::value)
                .isEqualTo(LockModeType.PESSIMISTIC_WRITE);
    }
}
