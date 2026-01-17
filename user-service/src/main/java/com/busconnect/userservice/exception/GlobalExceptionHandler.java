package com.busconnect.userservice.exception;

import com.busconnect.userservice.dto.error.ErrorResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
/**
 * Global handler for exceptions in the microservice (Reactive version).
 * Returns responses with standard format and localized messages.
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    /**
     * Handles EmailAlreadyExistsException.
     *
     * @param ex the exception
     * @return Mono with error response
     */
    @ExceptionHandler(EmailAlreadyExistsException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleEmailAlreadyExistsException(EmailAlreadyExistsException ex) {
        log.error("EmailAlreadyExistsException: {}", ex.getMessage());

        return Mono.fromCallable(() ->{
            String localizedMessage = messageSource.getMessage(
                    ex.getMessage(),
                    null,
                    ex.getMessage(),
                    LocaleContextHolder.getLocale()
            );

            ErrorResponse errorResponse = new ErrorResponse(
                    HttpStatus.CONFLICT.value(),
                    "USER_EMAIL_EXISTS",
                    localizedMessage,
                    LocalDateTime.now()
            );

            return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
        });

    }

    /**
     * Handles UserNotFoundException.
     *
     * @param ex the exception
     * @return Mono with error response
     */
    @ExceptionHandler(UserNotFoundException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleUserNotFoundException(UserNotFoundException ex) {
        log.error("UserNotFoundException: {}", ex.getMessage());

        return Mono.fromCallable(() ->{
            String localizedMessage = messageSource.getMessage(
                    ex.getMessage(),
                    null,
                    ex.getMessage(),
                    LocaleContextHolder.getLocale()
            );

            ErrorResponse errorResponse = new ErrorResponse(
                    HttpStatus.NOT_FOUND.value(),
                    "USER_NOT_FOUND",
                    localizedMessage,
                    LocalDateTime.now()
            );

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        });
    }

    /**
     * Handles UserAlreadyActiveException.
     *
     * @param ex the exception
     * @return Mono with error response
     */
    @ExceptionHandler(UserAlreadyActiveException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleUserAlreadyActiveException(UserAlreadyActiveException ex) {
        log.error("UserAlreadyActiveException: {}", ex.getMessage());

        return Mono.fromCallable(() ->{
            String localizedMessage = messageSource.getMessage(
                    ex.getMessage(),
                    null,
                    ex.getMessage(),
                    LocaleContextHolder.getLocale()
            );

            ErrorResponse errorResponse = new ErrorResponse(
                    HttpStatus.BAD_REQUEST.value(),
                    "USER_ALREADY_ACTIVE",
                    localizedMessage,
                    LocalDateTime.now()
            );

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        });
    }

    /**
     * Handles generic exceptions as fallback.
     *
     * @param ex the exception
     * @return Mono with error response
     */
    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ErrorResponse>> handleGenericException(Exception ex) {
        log.error("Unhandled exception: ", ex);

        return Mono.fromCallable(() -> {
            ErrorResponse errorResponse = new ErrorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "INTERNAL_SERVER_ERROR",
                    "An unexpected error occurred. Please contact support if this persists.",
                    LocalDateTime.now()
            );

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        });
    }
}
