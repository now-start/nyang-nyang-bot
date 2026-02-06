package org.nowstart.nyangnyangbot.data.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponseDto<T> {

    private int code;
    private String message;
    private T content;
}
