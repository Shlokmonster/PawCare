package com.pawcare.exception;

import java.util.List;

/** Raised when a vaccination record fails validation. */
public class InvalidVaccinationException extends PawCareException {

    private static final long serialVersionUID = 1L;

    public InvalidVaccinationException(String message) {
        super(message);
    }

    public InvalidVaccinationException(String message, List<String> details) {
        super(message, details);
    }
}
