package org.nowstart.nyangnyangbot.data.dto.chzzk;

public record ApiResponseDto<T>(int code, String message, T content) {
}
