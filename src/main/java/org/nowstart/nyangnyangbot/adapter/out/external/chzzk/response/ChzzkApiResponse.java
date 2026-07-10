package org.nowstart.nyangnyangbot.adapter.out.external.chzzk.response;

public record ChzzkApiResponse<T>(Integer code, String message, T content) {
}
