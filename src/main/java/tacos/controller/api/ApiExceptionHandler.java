package tacos.controller.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import tacos.domain.InvalidOrderStatusTransitionException;
import tacos.domain.OrderNotEditableException;
import tacos.service.InvalidOrderFilterException;
import tacos.service.InvalidOrderRequestException;
import tacos.service.OrderVersionConflictException;
import tacos.service.ResourceNotFoundException;
import tacos.service.TacoUnavailableException;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = "tacos.controller.api")
public class ApiExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(
            ResourceNotFoundException exception,
            HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, "Resource not found", exception.getMessage(), request);
    }

    @ExceptionHandler({
            InvalidOrderStatusTransitionException.class,
            OrderNotEditableException.class,
            OrderVersionConflictException.class,
            TacoUnavailableException.class
    })
    public ProblemDetail handleConflict(Exception exception, HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, "Order conflict", exception.getMessage(), request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrityConflict(
            DataIntegrityViolationException exception,
            HttpServletRequest request) {
        return problem(
                HttpStatus.CONFLICT,
                "Data conflict",
                "The request conflicts with the current state of the data.",
                request);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLock(
            ObjectOptimisticLockingFailureException exception,
            HttpServletRequest request) {
        return problem(
                HttpStatus.CONFLICT,
                "Order conflict",
                "The order was updated concurrently. Reload it and retry.",
                request);
    }

    @ExceptionHandler({InvalidOrderFilterException.class, InvalidOrderRequestException.class})
    public ProblemDetail handleBadRequest(Exception exception, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid request", exception.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {
        ProblemDetail detail = problem(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                "One or more request fields are invalid.",
                request);
        Map<String, List<String>> violations = new LinkedHashMap<>();
        for (FieldError error : exception.getBindingResult().getFieldErrors()) {
            violations.computeIfAbsent(error.getField(), ignored -> new java.util.ArrayList<>())
                    .add(error.getDefaultMessage());
        }
        detail.setProperty("violations", violations);
        return detail;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "Validation failed", exception.getMessage(), request);
    }

    @ExceptionHandler({
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class
    })
    public ProblemDetail handleMalformedRequest(Exception exception, HttpServletRequest request) {
        return problem(
                HttpStatus.BAD_REQUEST,
                "Malformed request",
                "The request contains a value with an invalid format.",
                request);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception exception, HttpServletRequest request) {
        log.error("Unhandled API error while processing {}", request.getRequestURI(), exception);
        return problem(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal server error",
                "An unexpected error occurred.",
                request);
    }

    private ProblemDetail problem(
            HttpStatus status,
            String title,
            String message,
            HttpServletRequest request) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(status, message);
        detail.setTitle(title);
        detail.setInstance(URI.create(request.getRequestURI()));
        return detail;
    }
}
