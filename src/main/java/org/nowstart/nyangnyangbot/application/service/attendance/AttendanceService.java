package org.nowstart.nyangnyangbot.application.service.attendance;

import io.micrometer.common.util.StringUtils;
import jakarta.transaction.Transactional;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.application.port.in.attendance.dto.AttendanceApplyCommand;
import org.nowstart.nyangnyangbot.application.port.in.attendance.dto.AttendanceApplyResult;
import org.nowstart.nyangnyangbot.application.port.in.attendance.dto.AttendanceUserSnapshot;
import org.nowstart.nyangnyangbot.application.port.in.favorite.dto.AdjustFavoriteCommand;
import org.nowstart.nyangnyangbot.application.port.in.favorite.usecase.GrantFavoriteUseCase;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.dto.ChatDto;
import org.nowstart.nyangnyangbot.domain.favorite.FavoriteSourceType;
import org.springframework.stereotype.Service;

@Service
@Transactional
@RequiredArgsConstructor
public class AttendanceService {

    private final GrantFavoriteUseCase grantFavoriteUseCase;
    private final Map<String, AttendanceUserDto> presence = new ConcurrentHashMap<>();
    private volatile boolean collecting = false;

    public void startCapture() {
        collecting = true;
        presence.clear();
    }

    public void stopCapture() {
        collecting = false;
        presence.clear();
    }

    public void recordChatUser(ChatDto chatDto) {
        if (chatDto == null) {
            return;
        }
        if (!collecting) {
            return;
        }
        String userId = chatDto.senderChannelId();
        if (StringUtils.isBlank(userId)) {
            return;
        }
        String nickName = chatDto.profile().nickname();
        long now = System.currentTimeMillis();
        presence.compute(userId, (key, existing) -> {
            if (existing == null) {
                return AttendanceUserDto.builder()
                        .userId(userId)
                        .nickName(nickName)
                        .lastMessageTime(now)
                        .build();
            }
            existing.setLastMessageTime(now);
            if (!StringUtils.isBlank(nickName)) {
                existing.setNickName(nickName);
            }
            return existing;
        });
    }

    public List<AttendanceUserSnapshot> getActiveUsers() {
        return presence.values().stream()
                .sorted(Comparator.comparingLong(AttendanceUserDto::getLastMessageTime).reversed())
                .map(user -> new AttendanceUserSnapshot(user.getUserId(), user.getNickName(), user.getLastMessageTime()))
                .toList();
    }

    public AttendanceApplyResult applyAttendance(AttendanceApplyCommand command) {
        if (!collecting) {
            throw new IllegalStateException("attendance cycle is not active");
        }
        List<AttendanceUserSnapshot> targets = resolveTargets(command);
        if (targets.isEmpty()) {
            throw new IllegalArgumentException("attendance targets are required");
        }
        int amount = resolveAmount(command);
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }

        for (AttendanceUserSnapshot target : targets) {
            if (target == null || StringUtils.isBlank(target.userId())) {
                continue;
            }
            grantFavoriteUseCase.grant(AdjustFavoriteCommand.builder()
                    .userId(target.userId())
                    .nickName(safeNickname(target))
                    .delta(amount)
                    .sourceType(FavoriteSourceType.ATTENDANCE)
                    .displayCategory("ATTENDANCE")
                    .publicDescription(String.format(Locale.ROOT, "출석체크(+%d)", amount))
                    .allowNegativeBalance(false)
                    .createIfMissing(true)
                    .build());
        }

        collecting = false;
        presence.clear();
        return new AttendanceApplyResult(amount, targets.size());
    }

    private List<AttendanceUserSnapshot> resolveTargets(AttendanceApplyCommand command) {
        if (command != null && command.users() != null && !command.users().isEmpty()) {
            return command.users();
        }
        return getActiveUsers();
    }

    private int resolveAmount(AttendanceApplyCommand command) {
        if (command == null || command.amount() == null) {
            return 1;
        }
        return command.amount();
    }

    private String safeNickname(AttendanceUserSnapshot user) {
        if (user == null || StringUtils.isBlank(user.nickName())) {
            return "";
        }
        return user.nickName();
    }
}
