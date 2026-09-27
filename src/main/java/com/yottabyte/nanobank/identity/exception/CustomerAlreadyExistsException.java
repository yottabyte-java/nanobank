package com.yottabyte.nanobank.identity.exception;

public class CustomerAlreadyExistsException
        extends RuntimeException {

    public CustomerAlreadyExistsException(String message) {
        super(message);
    }
}
