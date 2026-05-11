package org.nowstart.nyangnyangbot.application.port.out.favorite.repository;

public interface CheckIdempotencyPort {

    boolean existsByIdempotencyKey(String idempotencyKey);
}
