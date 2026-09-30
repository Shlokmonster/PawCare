package com.pawcare.exception;

/** Raised when a record referenced by id does not exist. */
public class EntityNotFoundException extends PawCareException {

    private static final long serialVersionUID = 1L;

    public EntityNotFoundException(String message) {
        super(message);
    }
}
