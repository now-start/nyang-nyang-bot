package org.nowstart.nyangnyangbot.application.chzzk.dto;

public record ApiResponseDto<T>(Integer code, String message, T content) {
}
