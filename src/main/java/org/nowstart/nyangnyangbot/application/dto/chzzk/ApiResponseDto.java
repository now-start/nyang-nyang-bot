package org.nowstart.nyangnyangbot.application.dto.chzzk;

public record ApiResponseDto<T>(Integer code, String message, T content) {
}
