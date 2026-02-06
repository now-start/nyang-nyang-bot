package org.nowstart.nyangnyangbot.data.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceApplyRequest {

    private List<AttendanceUserDto> users;
    private Integer amount;
}
