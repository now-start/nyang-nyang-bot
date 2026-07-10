package org.nowstart.nyangnyangbot.adapter.in.web.favorite;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.application.port.in.favorite.ManageFavoriteAdjustmentUseCase;
import org.nowstart.nyangnyangbot.application.port.in.favorite.ManageFavoriteAdjustmentUseCase.FavoriteAdjustmentApplyCommand;
import org.nowstart.nyangnyangbot.application.port.in.favorite.ManageFavoriteAdjustmentUseCase.FavoriteAdjustmentApplyResult;
import org.nowstart.nyangnyangbot.application.port.in.favorite.ManageFavoriteAdjustmentUseCase.FavoriteAdjustmentOptionResult;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;

@ExtendWith(MockitoExtension.class)
class FavoriteAdjustmentControllerTest {

    @Mock
    private ManageFavoriteAdjustmentUseCase manageFavoriteAdjustmentUseCase;

    @InjectMocks
    private FavoriteAdjustmentController controller;

    @Test
    void getAdjustments_ShouldReturnAdjustmentListFragment() {
        // 준비
        given(manageFavoriteAdjustmentUseCase.getAdjustments())
                .willReturn(List.of(new FavoriteAdjustmentOptionResult(1L, 5, "출석 보너스")));
        ExtendedModelMap model = new ExtendedModelMap();

        // 실행
        String view = controller.getAdjustments(model);

        // 검증
        then(view).isEqualTo("features/favorite/overlays :: adjustment-list");
        then((List<?>) model.get("adjustments")).hasSize(1);
    }

    @Test
    void getAdjustmentModal_ShouldReturnModalContentFragment() {
        // 준비
        given(manageFavoriteAdjustmentUseCase.getAdjustments())
                .willReturn(List.of(new FavoriteAdjustmentOptionResult(1L, 5, "출석 보너스")));
        ExtendedModelMap model = new ExtendedModelMap();
        FavoriteAdjustmentController.FavoriteAdjustmentTarget target =
                new FavoriteAdjustmentController.FavoriteAdjustmentTarget("user1", "치즈냥", 10);

        // 실행
        String view = controller.getAdjustmentModal(target, model);

        // 검증
        then(view).isEqualTo("features/favorite/overlays :: karma-modal-content");
        then(model.get("target")).isEqualTo(target);
        then((List<?>) model.get("adjustments")).hasSize(1);
    }

    @Test
    void applyAdjustments_ShouldReturnSuccessFeedbackAndRefreshTrigger() {
        // 준비
        given(manageFavoriteAdjustmentUseCase.applyAdjustments(any(FavoriteAdjustmentApplyCommand.class)))
                .willReturn(new FavoriteAdjustmentApplyResult("user1", 10, 5, 15, "보너스"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        ExtendedModelMap model = new ExtendedModelMap();
        FavoriteAdjustmentController.FavoriteAdjustmentApplyForm form =
                new FavoriteAdjustmentController.FavoriteAdjustmentApplyForm("user1", List.of(1L), null, null);

        // 실행
        String view = controller.applyAdjustments(form, bindingResult(form), response, model);

        // 검증
        then(view).isEqualTo("components/feedback :: alert");
        then(model.get("message")).isEqualTo("업보 적용 완료");
        then(model.get("tone")).isEqualTo("success");
        then(response.getHeader("HX-Trigger")).contains("favorite-board-refresh");

        ArgumentCaptor<FavoriteAdjustmentApplyCommand> captor = ArgumentCaptor.forClass(FavoriteAdjustmentApplyCommand.class);
        org.mockito.BDDMockito.then(manageFavoriteAdjustmentUseCase).should().applyAdjustments(captor.capture());
        then(captor.getValue().userId()).isEqualTo("user1");
        then(captor.getValue().adjustmentIds()).containsExactly(1L);
    }

    @Test
    void applyAdjustments_ShouldReturnFailureFeedback_WhenServiceFails() {
        // 준비
        given(manageFavoriteAdjustmentUseCase.applyAdjustments(any(FavoriteAdjustmentApplyCommand.class)))
                .willThrow(new IllegalArgumentException("invalid"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        ExtendedModelMap model = new ExtendedModelMap();

        // 실행
        FavoriteAdjustmentController.FavoriteAdjustmentApplyForm form =
                new FavoriteAdjustmentController.FavoriteAdjustmentApplyForm("user1", List.of(), 0, null);
        String view = controller.applyAdjustments(
                form,
                bindingResult(form),
                response,
                model
        );

        // 검증
        then(view).isEqualTo("components/feedback :: alert");
        then(model.get("message")).isEqualTo("업보 적용 실패");
        then(model.get("tone")).isEqualTo("danger");
        then(response.getHeader("HX-Trigger")).isNull();
    }

    private BindingResult bindingResult(Object form) {
        return new BeanPropertyBindingResult(form, "form");
    }
}
