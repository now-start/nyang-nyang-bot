package org.nowstart.nyangnyangbot.adapter.in.web.error;

import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 컨트롤러 전 영역에 공통으로 적용되는 예외 처리. 처리되지 않은 예외가 스택트레이스와 함께
 * 5xx로 노출되는 것을 막고, 도메인 검증 실패와 데이터 무결성 충돌을 의미 있는 상태 코드로 매핑한다.
 * 인증/인가(401/403) 예외는 Spring Security 필터 체인이 처리하므로 여기서 다루지 않는다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException exception) {
        log.warn("[400] 잘못된 요청: {}", exception.getMessage());
        return response(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.warn("[400] 요청 본문 검증 실패: {}", message);
        return response(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException exception) {
        // 멱등성 충돌·중복 INSERT 등은 내부 SQL 메시지를 노출하지 않고 충돌로만 응답한다.
        log.warn("[409] 데이터 무결성 위반", exception);
        return response(HttpStatus.CONFLICT, "이미 처리되었거나 중복된 요청입니다.");
    }

    private ResponseEntity<ErrorResponse> response(HttpStatus status, String message) {
        String safeMessage = (message == null || message.isBlank()) ? status.getReasonPhrase() : message;
        return ResponseEntity.status(status)
                .body(new ErrorResponse(status.value(), status.getReasonPhrase(), safeMessage));
    }
}
