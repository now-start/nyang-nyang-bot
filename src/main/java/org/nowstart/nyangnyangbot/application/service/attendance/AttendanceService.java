package org.nowstart.nyangnyangbot.application.service.attendance;

import io.micrometer.common.util.StringUtils;
import jakarta.transaction.Transactional;
import java.util.HashMap;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.application.port.in.attendance.ManageAttendanceUseCase;
import org.nowstart.nyangnyangbot.application.port.in.attendance.RecordAttendanceChatUseCase;
import org.nowstart.nyangnyangbot.application.port.in.chzzk.HandleChzzkEventUseCase.ChatReceived;
import org.nowstart.nyangnyangbot.application.port.in.favorite.AdjustFavoriteUseCase.AdjustFavoriteCommand;
import org.nowstart.nyangnyangbot.application.port.in.favorite.GrantFavoriteUseCase;
import org.nowstart.nyangnyangbot.application.service.chat.ChatEventSupport;
import org.nowstart.nyangnyangbot.application.validation.UseCaseValidator;
import org.nowstart.nyangnyangbot.domain.attendance.AttendanceUserState;
import org.nowstart.nyangnyangbot.domain.favorite.FavoriteSourceType;
import org.springframework.stereotype.Service;

@Service
@Transactional
@RequiredArgsConstructor
public class AttendanceService implements ManageAttendanceUseCase, RecordAttendanceChatUseCase {

    private final GrantFavoriteUseCase grantFavoriteUseCase;
    private final UseCaseValidator useCaseValidator;
    private final ReentrantLock cycleLock = new ReentrantLock();
    private AttendanceCycle cycle = AttendanceCycle.inactive();

    @Override
    public void startCapture() {
        cycleLock.lock();
        try {
            cycle = AttendanceCycle.start();
        } finally {
            cycleLock.unlock();
        }
    }

    @Override
    public void stopCapture() {
        cycleLock.lock();
        try {
            cycle = AttendanceCycle.inactive();
        } finally {
            cycleLock.unlock();
        }
    }

    @Override
    public void recordChatUser(ChatReceived chat) {
        if (chat == null) {
            return;
        }
        if (!ChatEventSupport.hasSenderChannelId(chat)) {
            return;
        }
        String userId = ChatEventSupport.senderChannelId(chat);
        String nickName = ChatEventSupport.displayName(chat);
        long now = System.currentTimeMillis();
        cycleLock.lock();
        try {
            if (!cycle.active()) {
                return;
            }
            cycle.users().compute(userId, (key, existing) -> new AttendanceUserState(
                    userId,
                    StringUtils.isBlank(nickName) && existing != null ? existing.nickName() : nickName,
                    now
            ));
        } finally {
            cycleLock.unlock();
        }
    }

    @Override
    public List<AttendanceUserSnapshot> getActiveUsers() {
        List<AttendanceUserState> users;
        cycleLock.lock();
        try {
            users = List.copyOf(cycle.users().values());
        } finally {
            cycleLock.unlock();
        }
        return users.stream()
                .sorted(Comparator.comparingLong(AttendanceUserState::lastMessageTime).reversed())
                .map(user -> new AttendanceUserSnapshot(user.userId(), user.nickName(), user.lastMessageTime()))
                .toList();
    }

    @Override
    public AttendanceApplyResult applyAttendance(AttendanceApplyCommand command) {
        useCaseValidator.validate(command, "command is required");
        List<AttendanceUserState> targets = closeCycle(command.userIds());
        int amount = command.amount();

        for (AttendanceUserState target : targets) {
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

        return new AttendanceApplyResult(amount, targets.size());
    }

    private List<AttendanceUserState> closeCycle(List<String> userIds) {
        cycleLock.lock();
        try {
            if (!cycle.active()) {
                throw new IllegalStateException("attendance cycle is not active");
            }
            List<AttendanceUserState> targets = userIds.stream()
                    .distinct()
                    .map(cycle.users()::get)
                    .filter(java.util.Objects::nonNull)
                    .toList();
            if (targets.isEmpty()) {
                throw new IllegalArgumentException("attendance targets are required");
            }
            cycle = AttendanceCycle.inactive();
            return targets;
        } finally {
            cycleLock.unlock();
        }
    }

    private String safeNickname(AttendanceUserState user) {
        if (StringUtils.isBlank(user.nickName())) {
            return "";
        }
        return user.nickName();
    }

    private record AttendanceCycle(
            boolean active,
            Map<String, AttendanceUserState> users
    ) {

        static AttendanceCycle start() {
            return new AttendanceCycle(true, new HashMap<>());
        }

        static AttendanceCycle inactive() {
            return new AttendanceCycle(false, Map.of());
        }
    }
}
