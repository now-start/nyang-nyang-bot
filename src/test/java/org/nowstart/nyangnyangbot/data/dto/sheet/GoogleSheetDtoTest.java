package org.nowstart.nyangnyangbot.data.dto.sheet;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class GoogleSheetDtoTest {

    @Test
    void fromRow_ShouldReturnDto_WhenFavoriteIsNumeric() {
        GoogleSheetDto dto = GoogleSheetDto.fromRow(List.of("닉네임", "user-1", 12));

        assertThat(dto).isEqualTo(new GoogleSheetDto("닉네임", "user-1", 12));
    }

    @Test
    void fromRow_ShouldReturnNull_WhenFavoriteIsBlank() {
        GoogleSheetDto dto = GoogleSheetDto.fromRow(List.of("닉네임", "user-1", " "));

        assertThat(dto).isNull();
    }

    @Test
    void fromRow_ShouldReturnNull_WhenFavoriteIsNotNumeric() {
        GoogleSheetDto dto = GoogleSheetDto.fromRow(List.of("닉네임", "user-1", "abc"));

        assertThat(dto).isNull();
    }
}
