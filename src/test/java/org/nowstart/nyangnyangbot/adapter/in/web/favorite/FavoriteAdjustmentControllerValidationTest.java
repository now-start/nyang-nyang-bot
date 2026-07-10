package org.nowstart.nyangnyangbot.adapter.in.web.favorite;

import static org.mockito.ArgumentMatchers.any;
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
import org.nowstart.nyangnyangbot.application.port.in.favorite.ManageFavoriteAdjustmentUseCase;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@ExtendWith(MockitoExtension.class)
class FavoriteAdjustmentControllerValidationTest {

    @Mock
    private ManageFavoriteAdjustmentUseCase manageFavoriteAdjustmentUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new FavoriteAdjustmentController(manageFavoriteAdjustmentUseCase))
                .setValidator(validator)
                .build();
    }

    @Test
    void applyAdjustments_ShouldRejectBlankUserBeforeUseCase() throws Exception {
        mockMvc.perform(post("/favorite/adjustments/apply")
                        .param("userId", " ")
                        .param("adjustmentIds", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("components/feedback :: alert"));

        verify(manageFavoriteAdjustmentUseCase, never()).applyAdjustments(any());
    }

    @Test
    void createAdjustment_ShouldRejectBlankLabelBeforeUseCase() throws Exception {
        mockMvc.perform(post("/favorite/adjustments")
                        .param("amount", "10")
                        .param("label", " "))
                .andExpect(status().isOk())
                .andExpect(view().name("features/favorite/overlays :: adjustment-list"));

        verify(manageFavoriteAdjustmentUseCase, never()).createAdjustment(any());
    }
}
