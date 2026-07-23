package org.nowstart.nyangnyangbot.adapter.in.web.presence;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.application.port.in.presence.ManagePresenceRewardUseCase;
import org.nowstart.nyangnyangbot.application.port.in.presence.ManagePresenceRewardUseCase.PresenceApplyCommand;
import org.nowstart.nyangnyangbot.application.port.in.presence.ManagePresenceRewardUseCase.PresenceApplyResult;
import org.nowstart.nyangnyangbot.application.port.in.presence.ManagePresenceRewardUseCase.PresenceUserSnapshot;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@ExtendWith(MockitoExtension.class)
class PresenceRewardControllerTest {

    @Mock
    private ManagePresenceRewardUseCase managePresenceRewardUseCase;

    private PresenceRewardController controller;

    @BeforeEach
    void setUp() {
        controller = new PresenceRewardController(managePresenceRewardUseCase);
    }

    @Test
    void getUsers_ShouldReturnPresenceFragmentAndSelectAllOnFirstLoad() {
        given(managePresenceRewardUseCase.getActiveUsers()).willReturn(List.of(
                new PresenceUserSnapshot("user1", "치즈냥", 1L),
                new PresenceUserSnapshot("user2", "후발냥", 2L)
        ));
        ExtendedModelMap model = new ExtendedModelMap();

        String view = controller.getUsers(null, null, false, model);

        then(view).isEqualTo("features/presence/components :: presence-list");
        then((List<?>) model.get("users")).hasSize(2);
        then(model.get("selectedUserIds")).isNull();
        then(model.get("knownUserIds")).isEqualTo(Set.of());
        then(model.get("selectedCount")).isEqualTo(2L);
        then(model.get("totalCount")).isEqualTo(2);
    }

    @Test
    void getUsers_ShouldPreserveSelectionAndAutomaticallySelectNewUsers() {
        given(managePresenceRewardUseCase.getActiveUsers()).willReturn(List.of(
                new PresenceUserSnapshot("user1", "치즈냥", 1L),
                new PresenceUserSnapshot("user2", "후발냥", 2L),
                new PresenceUserSnapshot("user3", "신규냥", 3L)
        ));
        ExtendedModelMap model = new ExtendedModelMap();

        String view = controller.getUsers(
                List.of("user2"),
                List.of("user1", "user2"),
                true,
                model
        );

        then(view).isEqualTo("features/presence/components :: presence-list");
        then(model.get("selectedUserIds")).isEqualTo(Set.of("user2"));
        then(model.get("knownUserIds")).isEqualTo(Set.of("user1", "user2"));
        then(model.get("selectedCount")).isEqualTo(2L);
        then(model.get("totalCount")).isEqualTo(3);
    }

    @Test
    void captureControls_ShouldUsePresenceFeedbackContract() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        ExtendedModelMap startModel = new ExtendedModelMap();

        String startView = controller.startCapture(response, startModel);

        then(startView).isEqualTo("features/presence/components :: presence-feedback-response");
        then(startModel.get("message")).isEqualTo("생존자 수집 시작");
        then(startModel.get("tone")).isEqualTo("success");
        then(startModel.get("resetPresenceList")).isEqualTo(false);
        then(response.getHeader("HX-Trigger")).isEqualTo("presence-users-refresh");
        ExtendedModelMap stopModel = new ExtendedModelMap();

        String stopView = controller.stopCapture(stopModel);

        then(stopView).isEqualTo("features/presence/components :: presence-feedback-response");
        then(stopModel.get("message")).isEqualTo("생존자 수집 종료");
        then(stopModel.get("tone")).isEqualTo("secondary");
        then(stopModel.get("resetPresenceList")).isEqualTo(true);
        BDDMockito.then(managePresenceRewardUseCase).should().startCapture();
        BDDMockito.then(managePresenceRewardUseCase).should().stopCapture();
    }

    @Test
    void applyPresenceReward_ShouldUseSelectedUsersAndRefreshPointBoard() {
        given(managePresenceRewardUseCase.applyPresenceReward(any(PresenceApplyCommand.class)))
                .willReturn(new PresenceApplyResult(5, 1));
        MockHttpServletResponse response = new MockHttpServletResponse();
        ExtendedModelMap model = new ExtendedModelMap();
        PresenceRewardController.PresenceRewardApplyForm form =
                new PresenceRewardController.PresenceRewardApplyForm(5L, List.of("user2"));

        String view = controller.applyPresenceReward(form, bindingResult(form), response, model);

        then(view).isEqualTo("features/presence/components :: presence-feedback-response");
        then(model.get("message")).isEqualTo("생존 확인 보상 완료");
        then(model.get("tone")).isEqualTo("success");
        then(model.get("resetPresenceList")).isEqualTo(true);
        then(response.getHeader("HX-Trigger")).contains("point-board-refresh");
        ArgumentCaptor<PresenceApplyCommand> captor = ArgumentCaptor.forClass(PresenceApplyCommand.class);
        BDDMockito.then(managePresenceRewardUseCase).should().applyPresenceReward(captor.capture());
        then(captor.getValue().amount()).isEqualTo(5L);
        then(captor.getValue().userIds()).containsExactly("user2");
    }

    @Test
    void applyPresenceReward_ShouldKeepSelection_WhenUseCaseFails() {
        given(managePresenceRewardUseCase.applyPresenceReward(any(PresenceApplyCommand.class)))
                .willThrow(new IllegalArgumentException("presence targets are required"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        ExtendedModelMap model = new ExtendedModelMap();
        PresenceRewardController.PresenceRewardApplyForm form =
                new PresenceRewardController.PresenceRewardApplyForm(5L, List.of("missing"));

        String view = controller.applyPresenceReward(form, bindingResult(form), response, model);

        then(view).isEqualTo("features/presence/components :: presence-feedback-response");
        then(model.get("message")).isEqualTo("생존자 보상 실패");
        then(model.get("tone")).isEqualTo("danger");
        then(model.get("resetPresenceList")).isEqualTo(false);
        then(response.getHeader("HX-Trigger")).isNull();
    }

    @Test
    void invalidForm_ShouldBeRejectedBeforeUseCase() throws Exception {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).setValidator(validator).build();

        mockMvc.perform(post("/presence-rewards/apply").param("amount", "0"))
                .andExpect(status().isOk())
                .andExpect(view().name("features/presence/components :: presence-feedback-response"));

        verify(managePresenceRewardUseCase, never()).applyPresenceReward(any());
    }

    private BindingResult bindingResult(Object form) {
        return new BeanPropertyBindingResult(form, "form");
    }
}
