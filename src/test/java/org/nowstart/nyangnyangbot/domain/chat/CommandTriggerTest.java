package org.nowstart.nyangnyangbot.domain.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class CommandTriggerTest {

    @Test
    void normalizeReturnsNullForMissingTrigger() {
        assertThat(CommandTrigger.normalize(null)).isNull();
        assertThat(CommandTrigger.normalize("   ")).isNull();
    }

    @Test
    void normalizeTrimsAndLowerCasesTrigger() {
        assertThat(CommandTrigger.normalize("  !HELLO  ")).isEqualTo("!hello");
    }

    @Test
    void normalizeUsesUnicodeNfc() {
        assertThat(CommandTrigger.normalize("!CAFE\u0301")).isEqualTo("!café");
    }

    @Test
    void validateRejectsInvalidTriggerShape() {
        assertThatThrownBy(() -> CommandTrigger.validate("command with spaces"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("trigger must start with !")
                .hasMessageContaining("trigger must be a single token");
        assertThatThrownBy(() -> CommandTrigger.validate("!"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("trigger length must be between 2 and 20");
    }
}
