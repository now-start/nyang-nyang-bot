package org.nowstart.nyangnyangbot.adapter.out.persistence.user;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.adapter.out.persistence.user.entity.UserAccount;
import org.nowstart.nyangnyangbot.adapter.out.persistence.user.repository.UserAccountRepository;
import org.nowstart.nyangnyangbot.adapter.out.validation.OutboundContractValidator;
import org.nowstart.nyangnyangbot.application.port.out.user.UserAccountPort;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserAccountPersistenceAdapter implements UserAccountPort {

    private final UserAccountRepository userAccountRepository;
    private final OutboundContractValidator contractValidator;

    @Override
    public void observe(ObserveUserCommand command) {
        contractValidator.request("userAccount.observe", command);
        userAccountRepository.observe(command.userId(), command.displayName());
    }

    @Override
    public Optional<String> findDisplayNameById(String userId) {
        return userAccountRepository.findById(userId).map(UserAccount::getDisplayName);
    }
}
