package org.nowstart.nyangnyangbot.data.type;

import static org.assertj.core.api.BDDAssertions.then;

import org.junit.jupiter.api.Test;

class CommandTypeTest {

    @Test
    void findNameByCommand_ShouldReturnLowercaseName_WhenMatched() {
        String result = CommandType.findNameByCommand(CommandType.FAVORITE.getCommand());

        then(result).isEqualTo("favorite");
    }

    @Test
    void findNameByCommand_ShouldReturnNull_WhenNotMatched() {
        String result = CommandType.findNameByCommand("not-a-command");

        then(result).isNull();
    }
}






