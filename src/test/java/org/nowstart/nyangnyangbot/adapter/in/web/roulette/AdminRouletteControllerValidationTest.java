package org.nowstart.nyangnyangbot.adapter.in.web.roulette;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
        given(manageRouletteUseCase.getConfigs(any())).willReturn(Page.empty());
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
}
