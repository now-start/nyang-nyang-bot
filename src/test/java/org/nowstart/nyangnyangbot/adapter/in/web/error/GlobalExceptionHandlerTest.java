package org.nowstart.nyangnyangbot.adapter.in.web.error;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
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
import org.nowstart.nyangnyangbot.application.validation.outbound.ExternalResponseContractException;
import org.nowstart.nyangnyangbot.application.validation.outbound.OutboundRequestContractException;
import org.nowstart.nyangnyangbot.application.validation.outbound.PersistenceDataContractException;

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

    @Test
    void handleConstraintViolation_ShouldReturnBadRequestWithPropertyPath() throws Exception {
        mockMvc.perform(get("/test/constraint-violation"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("userId: userId is required"));
    }

    @Test
    void handleOutboundRequestContract_ShouldReturnInternalServerErrorWithoutDetails() throws Exception {
        mockMvc.perform(get("/test/outbound-request-contract"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("내부 요청 계약을 처리하지 못했습니다."));
    }

    @Test
    void handleExternalResponseContract_ShouldReturnBadGatewayWithoutDetails() throws Exception {
        mockMvc.perform(get("/test/external-response-contract"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value("외부 시스템 응답 계약이 올바르지 않습니다."));
    }

    @Test
    void handlePersistenceDataContract_ShouldReturnInternalServerErrorWithoutDetails() throws Exception {
        mockMvc.perform(get("/test/persistence-data-contract"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("저장 데이터 계약이 올바르지 않습니다."));
    }

    @RestController
    static class ThrowingController {

        @GetMapping("/test/illegal-argument")
        String illegalArgument() {
            throw new IllegalArgumentException("delta must not be zero");
        }

        @GetMapping("/test/data-integrity")
        String dataIntegrity() {
            throw new DataIntegrityViolationException("duplicate key uk_point_ledger_entry__idempotency");
        }

        @GetMapping("/test/constraint-violation")
        String constraintViolation() {
            var validator = Validation.buildDefaultValidatorFactory().getValidator();
            throw new ConstraintViolationException(validator.validate(new FieldValidationRequest("")));
        }

        @GetMapping("/test/outbound-request-contract")
        String outboundRequestContract() {
            throw new OutboundRequestContractException("secret contract detail");
        }

        @GetMapping("/test/external-response-contract")
        String externalResponseContract() {
            throw new ExternalResponseContractException("secret upstream detail");
        }

        @GetMapping("/test/persistence-data-contract")
        String persistenceDataContract() {
            throw new PersistenceDataContractException("secret database detail");
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
