package org.nowstart.nyangnyangbot.application.service.user;

import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.application.port.in.user.ObserveUserUseCase;
import org.nowstart.nyangnyangbot.application.port.out.user.UserAccountPort;
import org.nowstart.nyangnyangbot.application.port.out.user.UserAccountPort.ObserveUserCommand;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class UserAccountService implements ObserveUserUseCase {

    private final UserAccountPort userAccountPort;

    @Override
    @Transactional
    public void observeUser(String userId, String displayName) {
        userAccountPort.observe(new ObserveUserCommand(userId, displayName));
    }
}
