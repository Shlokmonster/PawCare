package com.pawcare.exception;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Base class of every error the clinic raises deliberately.
 *
 * <p>It is a <b>checked</b> exception ({@code extends Exception}), which forces the GUI
 * layer to handle validation failures instead of letting them disappear silently. A
 * single exception can carry several messages, so a form can report every problem at
 * once rather than one field at a time.</p>
 */
public class PawCareException extends Exception {

    private static final long serialVersionUID = 1L;

    private final List<String> details;

    public PawCareException(String message) {
        this(message, Collections.emptyList());
    }

    public PawCareException(String message, List<String> details) {
        super(message);
        this.details = details == null ? Collections.emptyList() : List.copyOf(details);
    }

    public PawCareException(String message, Throwable cause) {
        super(message, cause);
        this.details = Collections.emptyList();
    }

    /** Individual validation problems, in the order they were detected. */
    public List<String> getDetails() {
        return details;
    }

    /** Message plus a bullet list of every underlying problem – used by error dialogs. */
    public String getDetailedMessage() {
        if (details.isEmpty()) {
            return getMessage();
        }
        return getMessage() + "\n"
                + details.stream().map(d -> "• " + d).collect(Collectors.joining("\n"));
    }
}
