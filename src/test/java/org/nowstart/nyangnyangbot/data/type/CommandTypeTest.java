package org.nowstart.nyangnyangbot.data.type;

import static org.assertj.core.api.BDDAssertions.then;

import org.junit.jupiter.api.Test;

class CommandTypeTest {

    @Test
    void findNameByCommand_ShouldReturnFavorite_WhenCommandMatches() {
        // when
        String result = CommandType.findNameByCommand("!호감도");

        // then
        then(result).isEqualTo("favorite");
    }

    @Test
    void findNameByCommand_ShouldReturnNull_WhenCommandNotFound() {
        // when
        String result = CommandType.findNameByCommand("!존재하지않는명령");

        // then
        then(result).isNull();
    }

    @Test
    void findNameByCommand_ShouldReturnNull_WhenCommandIsNull() {
        // when
        String result = CommandType.findNameByCommand(null);

        // then
        then(result).isNull();
    }

    @Test
    void findNameByCommand_ShouldReturnNull_WhenCommandIsEmpty() {
        // when
        String result = CommandType.findNameByCommand("");

        // then
        then(result).isNull();
    }

    @Test
    void getCommand_ShouldReturnCorrectCommand() {
        // when
        String command = CommandType.FAVORITE.getCommand();

        // then
        then(command).isEqualTo("!호감도");
    }

    @Test
    void findNameByCommand_ShouldReturnLowerCase() {
        // when
        String result = CommandType.findNameByCommand("!호감도");

        // then
        then(result).isLowerCase();
        then(result).isEqualTo("favorite");
    }

    @Test
    void findNameByCommand_ShouldBeCaseSensitive() {
        // given - CommandType only has "!호감도", not "!호감도!" or variations

        // when
        String result = CommandType.findNameByCommand("!호감도!");

        // then
        then(result).isNull();
    }

    @Test
    void commandType_ShouldHaveFavoriteEnum() {
        // when
        CommandType favorite = CommandType.FAVORITE;

        // then
        then(favorite).isNotNull();
        then(favorite.getCommand()).isEqualTo("!호감도");
    }

    @Test
    void values_ShouldReturnAllCommandTypes() {
        // when
        CommandType[] values = CommandType.values();

        // then
        then(values).isNotEmpty();
        then(values).contains(CommandType.FAVORITE);
    }

    @Test
    void valueOf_ShouldReturnCorrectEnum() {
        // when
        CommandType result = CommandType.valueOf("FAVORITE");

        // then
        then(result).isEqualTo(CommandType.FAVORITE);
        then(result.getCommand()).isEqualTo("!호감도");
    }

    @Test
    void findNameByCommand_ShouldHandleWhitespace() {
        // when
        String result = CommandType.findNameByCommand(" !호감도 ");

        // then
        then(result).isNull(); // Exact match required
    }

    @Test
    void findNameByCommand_ShouldWorkWithExactMatch() {
        // given
        String exactCommand = CommandType.FAVORITE.getCommand();

        // when
        String result = CommandType.findNameByCommand(exactCommand);

        // then
        then(result).isEqualTo("favorite");
    }
}
