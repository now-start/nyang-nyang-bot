package org.nowstart.nyangnyangbot.adapter.out.validation;

import static org.assertj.core.api.BDDAssertions.thenThrownBy;

import jakarta.validation.Validation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.Test;
import org.nowstart.nyangnyangbot.application.validation.outbound.ExternalResponseContractException;
import org.nowstart.nyangnyangbot.application.validation.outbound.OutboundRequestContractException;
import org.nowstart.nyangnyangbot.application.validation.outbound.OutboundResult;
import org.nowstart.nyangnyangbot.application.validation.outbound.PersistenceDataContractException;

class OutboundContractValidatorTest {

    private final OutboundContractValidator validator = new OutboundContractValidator(
            Validation.buildDefaultValidatorFactory().getValidator()
    );

    @Test
    void request_ShouldRejectInvalidOutboundCommand() {
        thenThrownBy(() -> validator.request("send", new Request(" ")))
                .isInstanceOf(OutboundRequestContractException.class)
                .hasMessageContaining("operation=send")
                .hasMessageContaining("value is required");
    }

    @Test
    void externalResponse_ShouldRejectInvalidExternalResult() {
        thenThrownBy(() -> validator.externalResponse("fetch", new Result(null)))
                .isInstanceOf(ExternalResponseContractException.class)
                .hasMessageContaining("operation=fetch")
                .hasMessageContaining("id is required");
    }

    @Test
    void persistenceResult_ShouldRejectInvalidDatabaseResult() {
        thenThrownBy(() -> validator.persistenceResult("load", new Result(null)))
                .isInstanceOf(PersistenceDataContractException.class)
                .hasMessageContaining("operation=load")
                .hasMessageContaining("id is required");
    }

    record Request(@NotBlank(message = "value is required") String value) {
    }

    record Result(@NotNull(groups = OutboundResult.class, message = "id is required") Long id) {
    }
}
