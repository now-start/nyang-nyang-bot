package org.nowstart.nyangnyangbot.application.service.upbo;

import jakarta.transaction.Transactional;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.application.port.in.upbo.ManageUpboUseCase.UserUpboResult;
import org.nowstart.nyangnyangbot.application.port.in.upbo.QueryUpboUseCase;
import org.nowstart.nyangnyangbot.application.port.out.upbo.UpboPort;
import org.nowstart.nyangnyangbot.application.port.out.upbo.UpboPort.UserResult;
import org.nowstart.nyangnyangbot.domain.type.UpboStatus;
import org.springframework.stereotype.Service;

@Service
@Transactional
@RequiredArgsConstructor
public class QueryUpboService implements QueryUpboUseCase {

    private final UpboPort upboPort;

    @Override
    public List<UserUpboResult> getUserUpbos(String userId, String status) {
        if (isBlank(userId)) {
            throw new IllegalArgumentException("userId is required");
        }
        UpboStatus parsedStatus = parseUpboStatus(status);
        return (parsedStatus == null
                ? upboPort.findUserUpbos(userId)
                : upboPort.findUserUpbosByStatus(userId, parsedStatus)).stream()
                .map(this::userUpboResult)
                .toList();
    }

    private UpboStatus parseUpboStatus(String value) {
        if (isBlank(value)) {
            return null;
        }
        return UpboStatus.valueOf(value.trim());
    }

    private UserUpboResult userUpboResult(UserResult userUpbo) {
        return new UserUpboResult(
                userUpbo.id(),
                userUpbo.userId(),
                userUpbo.nickNameSnapshot(),
                userUpbo.label(),
                userUpbo.status() == null ? null : userUpbo.status().name(),
                userUpbo.exchangeFavoriteValue(),
                userUpbo.rewardType() == null ? null : userUpbo.rewardType().name(),
                userUpbo.conversionMode() == null ? null : userUpbo.conversionMode().name(),
                userUpbo.ledgerId(),
                userUpbo.publicDescription(),
                userUpbo.createdAt()
        );
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
