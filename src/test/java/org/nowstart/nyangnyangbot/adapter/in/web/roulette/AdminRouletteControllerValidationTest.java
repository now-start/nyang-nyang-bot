package org.nowstart.nyangnyangbot.adapter.in.web.roulette;

import static org.nowstart.nyangnyangbot.domain.roulette.RoulettePolicy.MAX_SIMULATION_ITERATIONS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.nowstart.nyangnyangbot.support.MethodValidationTestSupport.validated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.application.port.in.roulette.ManageRouletteUseCase;
import org.nowstart.nyangnyangbot.application.port.in.roulette.QueryRouletteResultUseCase;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.ui.ConcurrentModel;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.data.domain.Page;

@ExtendWith(MockitoExtension.class)
class AdminRouletteControllerValidationTest {

    @Mock
    private ManageRouletteUseCase manageRouletteUseCase;

    @Mock
    private QueryRouletteResultUseCase queryRouletteResultUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.lenient()
                .when(manageRouletteUseCase.getConfigs(any()))
                .thenReturn(Page.empty());
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AdminRouletteController(manageRouletteUseCase, queryRouletteResultUseCase))
                .setValidator(validator)
                .build();
    }

    @Test
    void createConfig_ShouldRejectInvalidFormBeforeUseCase() throws Exception {
        mockMvc.perform(post("/admin/roulette/configs")
                        .param("title", " ")
                        .param("triggerToken", "command")
                        .param("pricePerRound", "0"))
                .andExpect(status().isOk())
                .andExpect(view().name("features/roulette/components :: roulette-config-region"));

        verify(manageRouletteUseCase, never()).createConfig(any());
    }

    @Test
    void addOption_ShouldRejectNonNumericProbabilityBeforeUseCase() throws Exception {
        mockMvc.perform(post("/admin/roulette/options")
                        .param("configId", "1")
                        .param("label", "꽝")
                        .param("probabilityPercent", "not-a-number"))
                .andExpect(status().isOk())
                .andExpect(view().name("features/roulette/components :: roulette-config-region"));

        verify(manageRouletteUseCase, never()).addOption(any());
    }

    @Test
    void addOption_ShouldRejectProbabilityOverOneHundredBeforeUseCase() throws Exception {
        mockMvc.perform(post("/admin/roulette/options")
                        .param("configId", "1")
                        .param("label", "꽝")
                        .param("probabilityPercent", "100.01"))
                .andExpect(status().isOk())
                .andExpect(view().name("features/roulette/components :: roulette-config-region"));

        verify(manageRouletteUseCase, never()).addOption(any());
    }

    @Test
    void simulate_ShouldRenderErrorFragmentStateForInvalidConfigId() {
        ManageRouletteUseCase validatedUseCase = validated(manageRouletteUseCase, ManageRouletteUseCase.class);
        AdminRouletteController controller = new AdminRouletteController(validatedUseCase, queryRouletteResultUseCase);
        ConcurrentModel model = new ConcurrentModel();

        String view = controller.simulate(0L, 100, model);

        org.assertj.core.api.Assertions.assertThat(view)
                .isEqualTo("features/roulette/components :: roulette-simulation");
        org.assertj.core.api.Assertions.assertThat(model.getAttribute("simulationError"))
                .isEqualTo("룰렛 시뮬레이션에 실패했습니다.");
        verify(manageRouletteUseCase, never()).simulate(any(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void simulate_ShouldClampIterationsBeforeCallingValidatedUseCase() {
        ManageRouletteUseCase validatedUseCase = validated(manageRouletteUseCase, ManageRouletteUseCase.class);
        AdminRouletteController controller = new AdminRouletteController(validatedUseCase, queryRouletteResultUseCase);

        controller.simulate(1L, MAX_SIMULATION_ITERATIONS + 1, new ConcurrentModel());

        verify(manageRouletteUseCase).simulate(1L, MAX_SIMULATION_ITERATIONS);
    }

    @Test
    void getConfigs_ShouldRenderEmptySelectionForInvalidSelectedConfigId() {
        ManageRouletteUseCase validatedUseCase = validated(manageRouletteUseCase, ManageRouletteUseCase.class);
        AdminRouletteController controller = new AdminRouletteController(validatedUseCase, queryRouletteResultUseCase);
        ConcurrentModel model = new ConcurrentModel();

        String view = controller.getConfigs(0L, 0, 20, model);

        org.assertj.core.api.Assertions.assertThat(view)
                .isEqualTo("features/roulette/components :: roulette-config-region");
        org.assertj.core.api.Assertions.assertThat(model.getAttribute("config")).isNull();
        verify(manageRouletteUseCase, never()).getConfig(any());
    }
}
