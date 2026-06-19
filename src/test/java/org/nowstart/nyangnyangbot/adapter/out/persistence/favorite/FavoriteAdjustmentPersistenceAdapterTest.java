package org.nowstart.nyangnyangbot.adapter.out.persistence.favorite;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.adapter.out.persistence.favorite.entity.FavoriteAdjustment;
import org.nowstart.nyangnyangbot.adapter.out.persistence.favorite.repository.FavoriteAdjustmentRepository;
import org.nowstart.nyangnyangbot.application.port.out.favorite.FavoriteAdjustmentPort.OptionResult;

@ExtendWith(MockitoExtension.class)
class FavoriteAdjustmentPersistenceAdapterTest {

    @Mock
    private FavoriteAdjustmentRepository favoriteAdjustmentRepository;

    @Test
    void findAndSave_ShouldMapAdjustmentOptions() {
        // 준비
        FavoriteAdjustmentPersistenceAdapter adapter = new FavoriteAdjustmentPersistenceAdapter(favoriteAdjustmentRepository);
        FavoriteAdjustment option = option(1L, 10, "보너스");
        given(favoriteAdjustmentRepository.findAll()).willReturn(List.of(option));
        given(favoriteAdjustmentRepository.findAllById(List.of(1L))).willReturn(List.of(option));
        given(favoriteAdjustmentRepository.save(any(FavoriteAdjustment.class))).willReturn(option);

        // 실행
        List<OptionResult> all = adapter.findAll();
        List<OptionResult> selected = adapter.findAllById(List.of(1L));
        OptionResult saved = adapter.save(10, "보너스");

        // 검증
        then(all).hasSize(1);
        then(selected.getFirst().id()).isEqualTo(1L);
        then(saved.amount()).isEqualTo(10);
        BDDMockito.then(favoriteAdjustmentRepository).should().save(any(FavoriteAdjustment.class));
    }

    private FavoriteAdjustment option(Long id, Integer amount, String label) {
        return FavoriteAdjustment.builder()
                .id(id)
                .amount(amount)
                .label(label)
                .build();
    }
}
