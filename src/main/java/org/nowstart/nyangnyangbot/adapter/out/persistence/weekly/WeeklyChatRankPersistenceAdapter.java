package org.nowstart.nyangnyangbot.adapter.out.persistence.weekly;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.application.model.WeeklyChatRankRecord;
import org.nowstart.nyangnyangbot.application.port.out.weekly.WeeklyChatRankPort;
import org.nowstart.nyangnyangbot.data.dto.WeeklyChatRankDto;
import org.nowstart.nyangnyangbot.data.entity.WeeklyChatRankEntity;
import org.nowstart.nyangnyangbot.repository.WeeklyChatRankRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WeeklyChatRankPersistenceAdapter implements WeeklyChatRankPort {

    private final WeeklyChatRankRepository weeklyChatRankRepository;

    @Override
    public Optional<WeeklyChatRankRecord> findByWeekStartDateAndUserId(LocalDate weekStartDate, String userId) {
        return weeklyChatRankRepository.findByWeekStartDateAndUserId(weekStartDate, userId)
                .map(this::toModel);
    }

    @Override
    public WeeklyChatRankRecord save(WeeklyChatRankRecord record) {
        WeeklyChatRankEntity entity = record.id() == null
                ? WeeklyChatRankEntity.builder().build()
                : weeklyChatRankRepository.findById(record.id()).orElseGet(WeeklyChatRankEntity.builder()::build);
        entity.setWeekStartDate(record.weekStartDate());
        entity.setUserId(record.userId());
        entity.setNickName(record.nickName());
        entity.setChatCount(record.chatCount());
        return toModel(weeklyChatRankRepository.save(entity));
    }

    @Override
    public List<WeeklyChatRankDto> findWeeklyRanks(LocalDate weekStartDate, int limit) {
        List<WeeklyChatRankRepository.WeeklyChatRankProjection> results =
                weeklyChatRankRepository.findWeeklyRanks(weekStartDate, PageRequest.of(0, limit));
        List<WeeklyChatRankDto> ranks = new ArrayList<>(results.size());
        int rank = 1;
        for (WeeklyChatRankRepository.WeeklyChatRankProjection result : results) {
            long chatCount = result.getChatCount() == null ? 0L : result.getChatCount();
            ranks.add(new WeeklyChatRankDto(rank++, result.getNickname(), chatCount));
        }
        return ranks;
    }

    private WeeklyChatRankRecord toModel(WeeklyChatRankEntity entity) {
        return new WeeklyChatRankRecord(
                entity.getId(),
                entity.getWeekStartDate(),
                entity.getUserId(),
                entity.getNickName(),
                entity.getChatCount()
        );
    }
}
