package org.nowstart.nyangnyangbot.adapter.out.external.chzzk.response;

import java.util.function.Function;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort.ApiResult;

public record ChzzkApiResponse<T>(Integer code, String message, T content) {

    public <R> ApiResult<R> toApiResult(Function<T, R> converter) {
        return new ApiResult<>(
                code,
                message,
                content == null ? null : converter.apply(content)
        );
    }
}
