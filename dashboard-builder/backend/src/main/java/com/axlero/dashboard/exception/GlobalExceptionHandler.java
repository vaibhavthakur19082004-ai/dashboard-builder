package com.axlero.dashboard.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.List;

/** Central place that turns every failure into a clean, standardized JSON error. */
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(InvalidQueryException.class)
    ResponseEntity<ErrorResponse> invalidQuery(InvalidQueryException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> validation(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage()).toList();
        return build(HttpStatus.BAD_REQUEST, "Validation failed", details);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MissingServletRequestParameterException.class})
    ResponseEntity<ErrorResponse> malformed(Exception ex) {
        return build(HttpStatus.BAD_REQUEST, "Malformed or incomplete request", List.of());
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ErrorResponse> unauthorized(AuthenticationException ex) {
        return build(HttpStatus.UNAUTHORIZED, "Invalid credentials", List.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ErrorResponse> forbidden(AccessDeniedException ex) {
        return build(HttpStatus.FORBIDDEN, "You do not have permission to perform this action", List.of());
    }

    @ExceptionHandler({ResourceNotFoundException.class, NoResourceFoundException.class})
    ResponseEntity<ErrorResponse> notFound(Exception ex) {
        return build(HttpStatus.NOT_FOUND, ex instanceof ResourceNotFoundException ? ex.getMessage() : "Resource not found", List.of());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ErrorResponse> method(HttpRequestMethodNotSupportedException ex) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, "HTTP method not supported", List.of());
    }

    @ExceptionHandler(ConflictException.class)
    ResponseEntity<ErrorResponse> conflict(ConflictException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), List.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErrorResponse> constraint(DataIntegrityViolationException ex) {
        log.warn("Database constraint violation", ex);
        return build(HttpStatus.CONFLICT, "The request conflicts with existing data", List.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> unexpected(Exception ex, HttpServletRequest req) {
        log.error("Unhandled error on {}", req.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error", List.of());
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, List<String> details) {
        return ResponseEntity.status(status)
                .body(new ErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(), message, details));
    }
}
