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
import org.nowstart.nyangnyangbot.data.dto.attendance.AttendanceDto;
import org.nowstart.nyangnyangbot.data.dto.attendance.AttendanceUserDto;
import org.nowstart.nyangnyangbot.data.dto.chzzk.ChatDto;
import org.nowstart.nyangnyangbot.data.entity.ChannelEntity;
import org.nowstart.nyangnyangbot.data.entity.FavoriteEntity;
import org.nowstart.nyangnyangbot.data.entity.FavoriteHistoryEntity;
import org.nowstart.nyangnyangbot.repository.FavoriteHistoryRepository;
import org.nowstart.nyangnyangbot.repository.FavoriteRepository;
import org.springframework.stereotype.Service;

@Service
@Transactional
@RequiredArgsConstructor
public class AttendanceService {

    private final FavoriteRepository favoriteRepository;
    private final FavoriteHistoryRepository favoriteHistoryRepository;
    private final ChannelService channelService;
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

    public List<AttendanceDto.User> getActiveUsers() {
        return presence.values().stream()
                .sorted(Comparator.comparingLong(AttendanceUserDto::getLastMessageTime).reversed())
                .map(user -> new AttendanceDto.User(user.getUserId(), user.getNickName(), user.getLastMessageTime()))
                .toList();
    }

    public AttendanceDto.ApplyResponse applyAttendance(AttendanceDto.ApplyRequest request) {
        List<AttendanceDto.User> targets = resolveTargets(request);
        if (targets.isEmpty()) {
            throw new IllegalArgumentException("attendance targets are required");
        }
        int amount = resolveAmount(request);
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        ChannelEntity ownerChannel = channelService.getDefaultChannel();

        for (AttendanceDto.User target : targets) {
            if (target == null || StringUtils.isBlank(target.userId())) {
                continue;
            }
            ChannelEntity targetChannel = channelService.getOrCreate(target.userId(), safeNickname(target));
            if (targetChannel == null) {
                continue;
            }
            FavoriteEntity favoriteEntity = favoriteRepository
                    .findByOwnerChannelIdAndTargetChannelId(ownerChannel.getId(), targetChannel.getId())
                    .orElseGet(() -> favoriteRepository.save(FavoriteEntity.builder()
                            .ownerChannel(ownerChannel)
                            .targetChannel(targetChannel)
                            .favorite(0)
                            .build()));

            int before = Objects.requireNonNullElse(favoriteEntity.getFavorite(), 0);
            int after = before + amount;
            favoriteEntity.setFavorite(after);
            if (!StringUtils.isBlank(target.nickName())) {
                favoriteEntity.setNickName(target.nickName());
            }
            favoriteHistoryRepository.save(FavoriteHistoryEntity.builder()
                    .favorite(favoriteEntity)
                    .history(String.format(Locale.ROOT, "출석체크(+%d)", amount))
                    .favoriteValue(after)
                    .build());
        }

        return new AttendanceDto.ApplyResponse(amount, targets.size());
    }

    private List<AttendanceDto.User> resolveTargets(AttendanceDto.ApplyRequest request) {
        if (request != null && request.users() != null && !request.users().isEmpty()) {
            return request.users();
        }
        return getActiveUsers();
    }

    private int resolveAmount(AttendanceDto.ApplyRequest request) {
        if (request == null || request.amount() == null) {
            return 1;
        }
        return request.amount();
    }

    private String safeNickname(AttendanceDto.User user) {
        if (user == null || StringUtils.isBlank(user.nickName())) {
            return "";
        }
        return user.nickName();
    }

}




