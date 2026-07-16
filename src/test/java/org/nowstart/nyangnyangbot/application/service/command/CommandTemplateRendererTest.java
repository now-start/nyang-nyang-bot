package org.nowstart.nyangnyangbot.application.service.command;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;

import java.util.Map;
import org.junit.jupiter.api.Test;

class CommandTemplateRendererTest {

    private final CommandTemplateRenderer renderer = new CommandTemplateRenderer();

    @Test
    void render_ShouldNeutralizeAndLimitResolvedValues() {
        // 실행
        String message = renderer.render(
                "{invocation.arg1}",
                Map.of(
                        "invocation.arg1",
                        "HTTP://example.com/@everyone-WWW.example.com-abcdefghijklmnopqrstuvwxyzabcdefghijklmnop"
                )
        );

        // 검증
        then(message).startsWith("http[:]//example.com/[at]everyone-www[.]example.com");
        then(message).hasSizeGreaterThan(60).hasSizeLessThanOrEqualTo(300);
    }

    @Test
    void render_ShouldLimitFinalMessageWithoutSplittingUnicodeCharacters() {
        // 실행
        String message = renderer.render("🐱".repeat(301), Map.of());

        // 검증
        then(message).isEqualTo("🐱".repeat(300));
        then(message.codePointCount(0, message.length())).isEqualTo(300);
    }

    @Test
    void render_ShouldRejectMissingAndMalformedVariables() {
        thenThrownBy(() -> renderer.render("{viewer.nickname}", Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("unresolved template variables: viewer.nickname");
        thenThrownBy(() -> renderer.render("{viewer.nickname", Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("malformed template variables: unmatched braces");
    }
}
