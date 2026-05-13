package org.nowstart.nyangnyangbot.application.port.out.favorite;

public interface CheckIdempotencyPort {

    boolean existsByIdempotencyKey(String idempotencyKey);
}
