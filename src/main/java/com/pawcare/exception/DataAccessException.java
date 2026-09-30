package com.pawcare.exception;

/**
 * Raised when persisted data cannot be read or written.
 *
 * <p>The application never swallows this silently: the storage layer wraps the original
 * {@code IOException} as the cause so the message shown to the user can explain what
 * happened while the stack trace remains available for debugging.</p>
 */
public class DataAccessException extends PawCareException {

    private static final long serialVersionUID = 1L;

    public DataAccessException(String message) {
        super(message);
    }

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
