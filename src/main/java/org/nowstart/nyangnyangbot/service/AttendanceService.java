package org.nowstart.nyangnyangbot.service;

import io.micrometer.common.util.StringUtils;
import jakarta.transaction.Transactional;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.data.dto.AttendanceApplyRequest;
import org.nowstart.nyangnyangbot.data.dto.AttendanceApplyResponse;
import org.nowstart.nyangnyangbot.data.dto.AttendanceUserDto;
import org.nowstart.nyangnyangbot.data.dto.ChatDto;
import org.nowstart.nyangnyangbot.data.entity.FavoriteEntity;
import org.nowstart.nyangnyangbot.data.entity.FavoriteHistoryEntity;
import org.nowstart.nyangnyangbot.repository.FavoriteRepository;
import org.springframework.stereotype.Service;

@Service
@Transactional
@RequiredArgsConstructor
public class AttendanceService {

    private static final long ACTIVE_WINDOW_MILLIS = 10 * 60 * 1000L;
    private final FavoriteRepository favoriteRepository;
    private final Map<String, AttendanceUser> presence = new ConcurrentHashMap<>();
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
        String userId = chatDto.getSenderChannelId();
        if (StringUtils.isBlank(userId)) {
            return;
        }
        String nickName = chatDto.getProfile().getNickname();
        long now = System.currentTimeMillis();
        presence.compute(userId, (key, existing) -> {
            if (existing == null) {
                return new AttendanceUser(userId, nickName, now);
            }
            existing.lastMessageTime = now;
            if (!StringUtils.isBlank(nickName)) {
                existing.nickName = nickName;
            }
            return existing;
        });
    }

    public List<AttendanceUserDto> getActiveUsers() {
        pruneInactive();
        return presence.values().stream()
                .sorted(Comparator.comparingLong(AttendanceUser::lastMessageTime).reversed())
                .map(user -> AttendanceUserDto.builder()
                        .userId(user.userId)
                        .nickName(user.nickName)
                        .lastMessageTime(user.lastMessageTime)
                        .build())
                .toList();
    }

    public AttendanceApplyResponse applyAttendance(AttendanceApplyRequest request) {
        List<AttendanceUserDto> targets = resolveTargets(request);
        if (targets.isEmpty()) {
            throw new IllegalArgumentException("attendance targets are required");
        }
        int amount = resolveAmount(request);
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }

        for (AttendanceUserDto target : targets) {
            if (target == null || StringUtils.isBlank(target.getUserId())) {
                continue;
            }
            FavoriteEntity favoriteEntity = favoriteRepository.findById(target.getUserId())
                    .orElseGet(() -> favoriteRepository.save(FavoriteEntity.builder()
                            .userId(target.getUserId())
                            .nickName(safeNickname(target))
                            .favorite(0)
                            .build()));

            int before = Objects.requireNonNullElse(favoriteEntity.getFavorite(), 0);
            int after = before + amount;
            favoriteEntity.setFavorite(after);
            if (!StringUtils.isBlank(target.getNickName())) {
                favoriteEntity.setNickName(target.getNickName());
            }
            favoriteEntity.getFavoriteHistoryEntityList().add(FavoriteHistoryEntity.builder()
                    .favoriteEntity(favoriteEntity)
                    .favorite(after)
                    .history(String.format(Locale.ROOT, "출석체크(+%d)", amount))
                    .build());
        }

        return AttendanceApplyResponse.builder()
                .amount(amount)
                .count(targets.size())
                .build();
    }

    private List<AttendanceUserDto> resolveTargets(AttendanceApplyRequest request) {
        if (request != null && request.getUsers() != null && !request.getUsers().isEmpty()) {
            return request.getUsers();
        }
        return getActiveUsers();
    }

    private int resolveAmount(AttendanceApplyRequest request) {
        if (request == null || request.getAmount() == null) {
            return 1;
        }
        return request.getAmount();
    }

    private String safeNickname(AttendanceUserDto user) {
        if (user == null || StringUtils.isBlank(user.getNickName())) {
            return "";
        }
        return user.getNickName();
    }

    private void pruneInactive() {
        long cutoff = System.currentTimeMillis() - ACTIVE_WINDOW_MILLIS;
        presence.entrySet().removeIf(entry -> entry.getValue().lastMessageTime < cutoff);
    }

    private static class AttendanceUser {
        private final String userId;
        private String nickName;
        private long lastMessageTime;

        private AttendanceUser(String userId, String nickName, long lastMessageTime) {
            this.userId = userId;
            this.nickName = nickName;
            this.lastMessageTime = lastMessageTime;
        }

        private long lastMessageTime() {
            return lastMessageTime;
        }
    }
}
