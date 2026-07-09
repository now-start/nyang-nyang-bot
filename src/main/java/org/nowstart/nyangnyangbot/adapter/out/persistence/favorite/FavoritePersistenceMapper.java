package org.nowstart.nyangnyangbot.adapter.out.persistence.favorite;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.nowstart.nyangnyangbot.adapter.out.persistence.favorite.entity.FavoriteHistory;
import org.nowstart.nyangnyangbot.application.port.out.favorite.FavoriteQueryPort.HistoryResult;
import org.nowstart.nyangnyangbot.application.port.out.favorite.FavoriteQueryPort.SummaryResult;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface FavoritePersistenceMapper {

    default org.nowstart.nyangnyangbot.domain.favorite.FavoriteAccount favoriteAccount(
            org.nowstart.nyangnyangbot.adapter.out.persistence.favorite.entity.FavoriteAccount entity
    ) {
        return org.nowstart.nyangnyangbot.domain.favorite.FavoriteAccount.of(
                entity.getUserId(),
                entity.getNickName(),
                entity.getFavorite()
        );
    }

    SummaryResult summaryResult(
            org.nowstart.nyangnyangbot.adapter.out.persistence.favorite.entity.FavoriteAccount entity
    );

    @Mapping(target = "ledgerId", source = "id")
    @Mapping(target = "channelId", expression = "java(channelId(entity))")
    @Mapping(target = "balanceAfter", expression = "java(balanceAfter(entity))")
    @Mapping(target = "publicDescription", expression = "java(publicDescription(entity))")
    @Mapping(target = "correction", expression = "java(entity.getCorrectionOfLedgerId() != null)")
    @Mapping(target = "createdAt", source = "createDate")
    HistoryResult historyResult(FavoriteHistory entity);

    default Integer balanceAfter(FavoriteHistory entity) {
        return entity.getBalanceAfter() == null ? entity.getFavorite() : entity.getBalanceAfter();
    }

    default String publicDescription(FavoriteHistory entity) {
        return entity.getPublicDescription() == null ? entity.getHistory() : entity.getPublicDescription();
    }

    default String channelId(FavoriteHistory entity) {
        return entity.getFavoriteAccount() == null ? null : entity.getFavoriteAccount().getUserId();
    }
}
