package org.nowstart.nyangnyangbot.adapter.in.web.error;

import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.nowstart.nyangnyangbot.application.validation.outbound.ExternalResponseContractException;
import org.nowstart.nyangnyangbot.application.validation.outbound.OutboundRequestContractException;
import org.nowstart.nyangnyangbot.application.validation.outbound.PersistenceDataContractException;

/**
 * REST 컨트롤러에 적용되는 예외 처리. 처리되지 않은 예외가 스택트레이스와 함께
 * 5xx로 노출되는 것을 막고, 입력 검증 실패와 데이터 무결성 충돌을 의미 있는 상태 코드로 매핑한다.
 * 인증/인가(401/403) 예외는 Spring Security 필터 체인이 처리하므로 여기서 다루지 않는다.
 */
@Slf4j
@RestControllerAdvice(annotations = RestController.class)
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException exception) {
        log.warn("[400] 잘못된 요청: {}", exception.getMessage());
        return response(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getAllErrors().stream()
                .map(this::validationMessage)
                .collect(Collectors.joining(", "));
        log.warn("[400] 요청 본문 검증 실패: {}", message);
        return response(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException exception) {
        // REST 클라이언트가 실패 필드를 식별할 수 있도록 프로퍼티 경로를 보존한다.
        String message = exception.getConstraintViolations().stream()
                .sorted((left, right) -> left.getPropertyPath().toString()
                        .compareTo(right.getPropertyPath().toString()))
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.joining(", "));
        log.warn("[400] 애플리케이션 입력 계약 검증 실패: {}", message);
        return response(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(OutboundRequestContractException.class)
    public ResponseEntity<ErrorResponse> handleOutboundRequestContract(OutboundRequestContractException exception) {
        log.error("[500] Port Out 요청 계약 위반", exception);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "내부 요청 계약을 처리하지 못했습니다.");
    }

    @ExceptionHandler(ExternalResponseContractException.class)
    public ResponseEntity<ErrorResponse> handleExternalResponseContract(ExternalResponseContractException exception) {
        log.error("[502] 외부 응답 계약 위반", exception);
        return response(HttpStatus.BAD_GATEWAY, "외부 시스템 응답 계약이 올바르지 않습니다.");
    }

    @ExceptionHandler(PersistenceDataContractException.class)
    public ResponseEntity<ErrorResponse> handlePersistenceDataContract(PersistenceDataContractException exception) {
        log.error("[500] 영속성 데이터 계약 위반", exception);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "저장 데이터 계약이 올바르지 않습니다.");
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

    private String validationMessage(ObjectError error) {
        if (error instanceof FieldError fieldError) {
            return fieldError.getField() + ": " + fieldError.getDefaultMessage();
        }
        return error.getDefaultMessage();
    }
}
