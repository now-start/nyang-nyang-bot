package org.nowstart.nyangnyangbot.adapter.in.web.attendance;

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
import org.nowstart.nyangnyangbot.application.port.in.attendance.ManageAttendanceUseCase;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@ExtendWith(MockitoExtension.class)
class AttendanceControllerValidationTest {

    @Mock
    private ManageAttendanceUseCase manageAttendanceUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AttendanceController(manageAttendanceUseCase))
                .setValidator(validator)
                .build();
    }

    @Test
    void applyAttendance_ShouldRejectInvalidFormBeforeUseCase() throws Exception {
        mockMvc.perform(post("/attendance/apply")
                        .param("amount", "0"))
                .andExpect(status().isOk())
                .andExpect(view().name("features/attendance/components :: attendance-feedback-response"));

        verify(manageAttendanceUseCase, never()).applyAttendance(any());
    }
}
