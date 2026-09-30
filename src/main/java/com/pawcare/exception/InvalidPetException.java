package com.pawcare.exception;

import java.util.List;

/** Raised when a pet record fails validation. */
public class InvalidPetException extends PawCareException {

    private static final long serialVersionUID = 1L;

    public InvalidPetException(String message) {
        super(message);
    }

    public InvalidPetException(String message, List<String> details) {
        super(message, details);
    }
}
