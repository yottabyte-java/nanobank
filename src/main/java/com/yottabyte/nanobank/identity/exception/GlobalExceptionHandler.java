package com.yottabyte.nanobank.identity.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

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
}
