package org.nowstart.nyangnyangbot.data.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ApiResponseDto<T> {

    private int code;
    private String message;
    private T content;
}
