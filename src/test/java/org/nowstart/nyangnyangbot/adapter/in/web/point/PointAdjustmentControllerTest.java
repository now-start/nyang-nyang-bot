package org.nowstart.nyangnyangbot.adapter.in.web.point;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.application.port.in.point.ManagePointAdjustmentPresetUseCase;
import org.nowstart.nyangnyangbot.application.port.in.point.ManagePointAdjustmentPresetUseCase.ApplyPointAdjustments;
import org.nowstart.nyangnyangbot.application.port.in.point.ManagePointAdjustmentPresetUseCase.PointAdjustmentPresetResult;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@ExtendWith(MockitoExtension.class)
class PointAdjustmentControllerTest {

    @Mock
    private ManagePointAdjustmentPresetUseCase managePointAdjustmentPresetUseCase;

    private PointAdjustmentController controller;

    @BeforeEach
    void setUp() {
        controller = new PointAdjustmentController(managePointAdjustmentPresetUseCase);
    }

    @Test
    void getAdjustments_ShouldReturnCanonicalPresetFragment() {
        given(managePointAdjustmentPresetUseCase.getPresets())
                .willReturn(List.of(new PointAdjustmentPresetResult(1L, 5, "생존 보너스")));
        ExtendedModelMap model = new ExtendedModelMap();

        String view = controller.getAdjustments(model);

        then(view).isEqualTo("features/point/overlays :: adjustment-list");
        then((List<?>) model.get("presets")).singleElement().isEqualTo(
                new PointAdjustmentController.PointAdjustmentPresetView(1L, 5, "생존 보너스")
        );
    }

    @Test
    void getAdjustmentModal_ShouldExposeTargetAndCanonicalPresetFragment() {
        given(managePointAdjustmentPresetUseCase.getPresets())
                .willReturn(List.of(new PointAdjustmentPresetResult(1L, -5, "차감")));
        ExtendedModelMap model = new ExtendedModelMap();
        PointAdjustmentController.PointAdjustmentTarget target =
                new PointAdjustmentController.PointAdjustmentTarget("user1", "치즈냥", 10L);

        String view = controller.getAdjustmentModal(target, model);

        then(view).isEqualTo("features/point/overlays :: point-adjustment-modal-content");
        then(model.get("target")).isEqualTo(target);
        then((List<?>) model.get("presets")).hasSize(1);
    }

    @Test
    void applyAdjustments_ShouldPassActorAndReturnPointRefreshTrigger() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        ExtendedModelMap model = new ExtendedModelMap();
        PointAdjustmentController.PointAdjustmentApplyForm form =
                new PointAdjustmentController.PointAdjustmentApplyForm(
                        "user1",
                        List.of(1L),
                        2L,
                        "수동 보너스"
                );

        String view = controller.applyAdjustments(
                form,
                bindingResult(form),
                response,
                new UsernamePasswordAuthenticationToken("admin", "N/A"),
                model
        );

        then(view).isEqualTo("components/feedback :: alert");
        then(model.get("message")).isEqualTo("포인트 조정 완료");
        then(model.get("tone")).isEqualTo("success");
        then(response.getHeader("HX-Trigger")).contains("point-board-refresh");
        ArgumentCaptor<ApplyPointAdjustments> captor = ArgumentCaptor.forClass(ApplyPointAdjustments.class);
        BDDMockito.then(managePointAdjustmentPresetUseCase).should().applyAdjustments(captor.capture());
        then(captor.getValue()).satisfies(command -> {
            then(command.userId()).isEqualTo("user1");
            then(command.presetIds()).containsExactly(1L);
            then(command.manualAmount()).isEqualTo(2L);
            then(command.manualDescription()).isEqualTo("수동 보너스");
            then(command.actorUserId()).isEqualTo("admin");
        });
    }

    @Test
    void applyAdjustments_ShouldReturnFailureWithoutRefresh_WhenUseCaseFails() {
        BDDMockito.willThrow(new IllegalArgumentException("invalid"))
                .given(managePointAdjustmentPresetUseCase)
                .applyAdjustments(any(ApplyPointAdjustments.class));
        MockHttpServletResponse response = new MockHttpServletResponse();
        ExtendedModelMap model = new ExtendedModelMap();
        PointAdjustmentController.PointAdjustmentApplyForm form =
                new PointAdjustmentController.PointAdjustmentApplyForm("user1", List.of(1L), null, null);

        String view = controller.applyAdjustments(form, bindingResult(form), response, null, model);

        then(view).isEqualTo("components/feedback :: alert");
        then(model.get("message")).isEqualTo("포인트 조정 실패");
        then(model.get("tone")).isEqualTo("danger");
        then(response.getHeader("HX-Trigger")).isNull();
    }

    @Test
    void invalidForms_ShouldBeRejectedBeforeUseCase() throws Exception {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).setValidator(validator).build();

        mockMvc.perform(post("/points/adjustments/apply")
                        .param("userId", " ")
                        .param("presetIds", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("components/feedback :: alert"));
        mockMvc.perform(post("/points/adjustments")
                        .param("amount", "10")
                        .param("label", " "))
                .andExpect(status().isOk())
                .andExpect(view().name("features/point/overlays :: adjustment-list"));

        verify(managePointAdjustmentPresetUseCase, never()).applyAdjustments(any());
        verify(managePointAdjustmentPresetUseCase, never()).createPreset(any());
    }

    private BindingResult bindingResult(Object form) {
        return new BeanPropertyBindingResult(form, "form");
    }
}
