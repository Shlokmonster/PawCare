package com.pawcare.exception;

import java.util.List;

/** Raised when an appointment fails validation, or clashes with an existing booking. */
public class InvalidAppointmentException extends PawCareException {

    private static final long serialVersionUID = 1L;

    public InvalidAppointmentException(String message) {
        super(message);
    }

    public InvalidAppointmentException(String message, List<String> details) {
        super(message, details);
    }
}
