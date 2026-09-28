package com.yottabyte.nanobank.identity.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(OnboardingNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(
            OnboardingNotFoundException exception
    ) {

        return new ErrorResponse(
                Instant.now(),
                404,
                "ONBOARDING_NOT_FOUND",
                exception.getMessage()
        );
    }

    @ExceptionHandler(CustomerAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleCustomerExists(
            CustomerAlreadyExistsException exception
    ) {

        return new ErrorResponse(
                Instant.now(),
                409,
                "CUSTOMER_ALREADY_EXISTS",
                exception.getMessage()
        );
    }

    @ExceptionHandler(InvalidOnboardingStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleInvalidState(
            InvalidOnboardingStateException exception
    ) {

        return new ErrorResponse(
                Instant.now(),
                409,
                "INVALID_ONBOARDING_STATE",
                exception.getMessage()
        );
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMissingHeader(
            MissingRequestHeaderException exception
    ) {

        return new ErrorResponse(
                Instant.now(),
                400,
                "MISSING_REQUIRED_HEADER",
                "Required header '" + exception.getHeaderName()
                        + "' is missing."
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleTypeMismatch(
            MethodArgumentTypeMismatchException exception
    ) {

        return new ErrorResponse(
                Instant.now(),
                400,
                "INVALID_PARAMETER",
                "Invalid value for '" + exception.getName() + "'."
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidation(
            MethodArgumentNotValidException exception
    ) {

        String message =
                exception.getBindingResult()
                        .getFieldErrors()
                        .stream()
                        .findFirst()
                        .map(error -> error.getField()
                                + ": " + error.getDefaultMessage())
                        .orElse("Request validation failed.");

        return new ErrorResponse(
                Instant.now(),
                400,
                "VALIDATION_FAILED",
                message
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleUnreadableBody(
            HttpMessageNotReadableException exception
    ) {

        return new ErrorResponse(
                Instant.now(),
                400,
                "MALFORMED_REQUEST_BODY",
                "Request body is missing or malformed."
        );
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleUnexpected(
            Exception exception
    ) {

        log.error("Unexpected error", exception);

        return new ErrorResponse(
                Instant.now(),
                500,
                "INTERNAL_SERVER_ERROR",
                "An unexpected error occurred."
        );
    }
}