package org.nowstart.nyangnyangbot.adapter.out.persistence.user;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.adapter.out.persistence.user.entity.UserAccount;
import org.nowstart.nyangnyangbot.adapter.out.persistence.user.repository.UserAccountRepository;
import org.nowstart.nyangnyangbot.application.port.out.user.UserAccountPort;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Validated
@RequiredArgsConstructor
public class UserAccountPersistenceAdapter implements UserAccountPort {

    private final UserAccountRepository userAccountRepository;
    @Override
    public void observe(ObserveUserCommand command) {
        userAccountRepository.observe(command.userId(), command.displayName());
    }

    @Override
    public Optional<String> findDisplayNameById(String userId) {
        return userAccountRepository.findById(userId).map(UserAccount::getDisplayName);
    }
}
