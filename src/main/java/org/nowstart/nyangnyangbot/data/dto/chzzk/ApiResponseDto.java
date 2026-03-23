package org.nowstart.nyangnyangbot.data.dto.chzzk;

public record ApiResponseDto<T>(Integer code, String message, T content) {
}
