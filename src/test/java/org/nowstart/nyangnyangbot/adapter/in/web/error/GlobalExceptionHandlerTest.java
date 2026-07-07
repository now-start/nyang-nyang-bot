package org.nowstart.nyangnyangbot.adapter.in.web.error;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ThrowingController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void handleIllegalArgument_ShouldReturnBadRequestWithMessage() throws Exception {
        mockMvc.perform(get("/test/illegal-argument"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("delta must not be zero"));
    }

    @Test
    void handleDataIntegrityViolation_ShouldReturnConflictWithoutLeakingInternals() throws Exception {
        mockMvc.perform(get("/test/data-integrity"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("이미 처리되었거나 중복된 요청입니다."));
    }

    @Test
    void handleMethodArgumentNotValid_ShouldKeepRealFieldPath() throws Exception {
        mockMvc.perform(post("/test/validation/field")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("userId: userId is required"));
    }

    @Test
    void handleMethodArgumentNotValid_ShouldKeepCommandFieldPath() throws Exception {
        mockMvc.perform(post("/test/validation/command")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("type: type is required"));
    }

    @RestController
    static class ThrowingController {

        @GetMapping("/test/illegal-argument")
        String illegalArgument() {
            throw new IllegalArgumentException("delta must not be zero");
        }

        @GetMapping("/test/data-integrity")
        String dataIntegrity() {
            throw new DataIntegrityViolationException("duplicate key uk_favorite_history_idempotency_key");
        }

        @PostMapping("/test/validation/field")
        String validationField(@Valid @RequestBody FieldValidationRequest request) {
            return request.userId();
        }

        @PostMapping("/test/validation/command")
        String validationCommand(@Valid @RequestBody CommandValidationRequest request) {
            return request.type();
        }
    }

    record FieldValidationRequest(
            @NotBlank(message = "userId is required")
            String userId
    ) {
    }

    record CommandValidationRequest(
            @NotBlank(message = "type is required")
            String type
    ) {
    }
}
