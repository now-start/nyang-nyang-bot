package org.nowstart.nyangnyangbot.application.port.out.point;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.Optional;
import org.nowstart.nyangnyangbot.domain.point.PointSourceType;

public interface PointLedgerPort {

    /**
     * 필요하면 사용자를 생성하고 표시 이름을 갱신한 뒤 현재 트랜잭션 동안 쓰기 잠금을 유지한다.
     *
     * @return 사용자가 존재하여 잠금을 획득했으면 {@code true}, 생성을 허용하지 않았고 사용자가 없으면
     *         {@code false}
     */
    boolean lockUser(String userId, String displayName, boolean createIfMissing);

    /** 사용자의 원장 잔액을 반환하며, 원장 항목이 없으면 0을 반환한다. */
    long balance(String userId);

    /** 멱등성 키로 생성된 원장 항목을 조회한다. */
    @Valid
    Optional<LedgerEntryRecord> findByIdempotencyKey(String idempotencyKey);

    /** 정정 대상 원장 항목을 조회하고 현재 트랜잭션 동안 쓰기 잠금을 유지한다. */
    @Valid
    Optional<LedgerEntryRecord> findCorrectionTargetForUpdate(long ledgerEntryId);

    /** 지정한 원장 항목을 참조하는 정정 항목이 이미 존재하는지 반환한다. */
    boolean hasCorrection(long ledgerEntryId);

    /** 변경할 수 없는 원장 항목을 추가하고 저장된 결과를 반환한다. */
    @Valid
    LedgerEntryRecord append(@Valid @NotNull(message = "point entry is required") AppendPointEntry data);

    record AppendPointEntry(
            @NotBlank(message = "userId is required") String userId,
            long delta,
            @NotNull(message = "sourceType is required") PointSourceType sourceType,
            String sourceReference,
            @NotBlank(message = "description is required") String description,
            String privateNote,
            Long correctionOfEntryId,
            String actorUserId,
            @NotBlank(message = "idempotencyKey is required") String idempotencyKey
    ) {
        @AssertTrue(message = "delta must not be zero")
        public boolean isDeltaNonZero() {
            return delta != 0;
        }
    }

    record LedgerEntryRecord(
            @Positive(message = "id must be positive") long id,
            @NotBlank(message = "userId is required") String userId,
            long delta,
            @NotNull(message = "sourceType is required") PointSourceType sourceType,
            String sourceReference,
            @NotBlank(message = "description is required") String description,
            String privateNote,
            Long correctionOfEntryId,
            String actorUserId
    ) {
        @AssertTrue(message = "delta must not be zero")
        public boolean isDeltaNonZero() {
            return delta != 0;
        }
    }
}
