package org.nowstart.nyangnyangbot.application.port.out.chzzk.dto;

public record ApiResponseDto<T>(Integer code, String message, T content) {
}
