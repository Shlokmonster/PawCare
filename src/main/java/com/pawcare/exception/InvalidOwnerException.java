package com.pawcare.exception;

import java.util.List;

/** Raised when an owner record fails validation. */
public class InvalidOwnerException extends PawCareException {

    private static final long serialVersionUID = 1L;

    public InvalidOwnerException(String message) {
        super(message);
    }

    public InvalidOwnerException(String message, List<String> details) {
        super(message, details);
    }
}
