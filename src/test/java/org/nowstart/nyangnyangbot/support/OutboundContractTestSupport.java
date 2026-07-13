package org.nowstart.nyangnyangbot.support;

import jakarta.validation.Validation;
import org.nowstart.nyangnyangbot.adapter.out.validation.OutboundContractValidator;

public final class OutboundContractTestSupport {

    private OutboundContractTestSupport() {
    }

    public static OutboundContractValidator outboundContractValidator() {
        return new OutboundContractValidator(
                Validation.buildDefaultValidatorFactory().getValidator()
        );
    }
}
