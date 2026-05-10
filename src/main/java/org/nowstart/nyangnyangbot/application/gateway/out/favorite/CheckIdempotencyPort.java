package org.nowstart.nyangnyangbot.application.gateway.out.favorite;

public interface CheckIdempotencyPort {

    boolean existsByIdempotencyKey(String idempotencyKey);
}
