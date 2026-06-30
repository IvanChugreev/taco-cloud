package tacos.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;
import tacos.service.DuplicateUsernameException;
import tacos.service.ResourceNotFoundException;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ModelAndView handleNotFound(ResourceNotFoundException exception, HttpServletRequest request) {
        return errorView(HttpStatus.NOT_FOUND, exception.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(DuplicateUsernameException.class)
    public ModelAndView handleConflict(DuplicateUsernameException exception, HttpServletRequest request) {
        return errorView(HttpStatus.CONFLICT, exception.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView handleUnexpected(Exception exception, HttpServletRequest request) {
        log.error("Unhandled error while processing {}", request.getRequestURI(), exception);
        return errorView(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again later.",
                request.getRequestURI());
    }

    private ModelAndView errorView(HttpStatus status, String message, String path) {
        ModelAndView modelAndView = new ModelAndView("error");
        modelAndView.setStatus(status);
        modelAndView.addObject("status", status.value());
        modelAndView.addObject("error", status.getReasonPhrase());
        modelAndView.addObject("message", message);
        modelAndView.addObject("path", path);
        return modelAndView;
    }
}
