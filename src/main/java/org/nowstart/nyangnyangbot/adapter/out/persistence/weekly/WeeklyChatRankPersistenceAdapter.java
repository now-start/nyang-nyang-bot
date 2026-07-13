package org.nowstart.nyangnyangbot.adapter.out.persistence.weekly;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.adapter.out.persistence.weekly.entity.WeeklyChatRank;
import org.nowstart.nyangnyangbot.adapter.out.persistence.weekly.repository.WeeklyChatRankRepository;
import org.nowstart.nyangnyangbot.adapter.out.validation.OutboundContractValidator;
import org.nowstart.nyangnyangbot.application.port.in.weeklychat.QueryWeeklyChatRankUseCase.WeeklyChatRankView;
import org.nowstart.nyangnyangbot.application.port.out.weekly.WeeklyChatRankPort;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WeeklyChatRankPersistenceAdapter implements WeeklyChatRankPort {

    private final WeeklyChatRankRepository weeklyChatRankRepository;
    private final OutboundContractValidator contractValidator;

    @Override
    public Optional<WeeklyChatRankRecordResult> findByWeekStartDateAndUserId(LocalDate weekStartDate, String userId) {
        return weeklyChatRankRepository.findByWeekStartDateAndUserId(weekStartDate, userId)
                .map(this::recordResult);
    }

    @Override
    public WeeklyChatRankRecordResult save(WeeklyChatRankRecordResult record) {
        contractValidator.request("weekly.save", record);
        WeeklyChatRank entity = record.id() == null
                ? WeeklyChatRank.builder().build()
                : weeklyChatRankRepository.findById(record.id()).orElseGet(WeeklyChatRank.builder()::build);
        entity.setWeekStartDate(record.weekStartDate());
        entity.setUserId(record.userId());
        entity.setNickName(record.nickName());
        entity.setChatCount(record.chatCount());
        return recordResult(weeklyChatRankRepository.save(entity));
    }

    @Override
    public List<WeeklyChatRankView> findWeeklyRanks(LocalDate weekStartDate, int limit) {
        List<WeeklyChatRankRepository.WeeklyChatRankProjection> results =
                weeklyChatRankRepository.findWeeklyRanks(weekStartDate, PageRequest.of(0, limit));
        List<WeeklyChatRankView> ranks = new ArrayList<>(results.size());
        int rank = 1;
        for (WeeklyChatRankRepository.WeeklyChatRankProjection result : results) {
            long chatCount = result.getChatCount() == null ? 0L : result.getChatCount();
            ranks.add(new WeeklyChatRankView(rank++, result.getNickname(), chatCount));
        }
        return ranks;
    }

    private WeeklyChatRankRecordResult recordResult(WeeklyChatRank entity) {
        return contractValidator.persistenceResult("weekly.recordResult", new WeeklyChatRankRecordResult(
                entity.getId(),
                entity.getWeekStartDate(),
                entity.getUserId(),
                entity.getNickName(),
                entity.getChatCount()
        ));
    }

}
