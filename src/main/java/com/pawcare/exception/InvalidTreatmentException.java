package com.pawcare.exception;

import java.util.List;

/** Raised when a treatment record fails validation. */
public class InvalidTreatmentException extends PawCareException {

    private static final long serialVersionUID = 1L;

    public InvalidTreatmentException(String message) {
        super(message);
    }

    public InvalidTreatmentException(String message, List<String> details) {
        super(message, details);
    }
}
