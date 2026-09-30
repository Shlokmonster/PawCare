package com.pawcare.gui.dialogs;

import com.pawcare.exception.PawCareException;

import java.awt.Component;
import java.awt.Window;

/**
 * One-line entry points for the messages the application shows.
 *
 * <p>Screens call {@code Dialogs.error(this, "…")} instead of assembling a
 * {@link MessageDialog} by hand, which keeps the wording of the standard prompts – and
 * the button labels that go with them – in one place.</p>
 */
public final class Dialogs {

    private Dialogs() {
    }

    public static void info(Component parent, String title, String message) {
        MessageDialog.showInfo(window(parent), title, message);
    }

    public static void success(Component parent, String title, String message) {
        MessageDialog.showSuccess(window(parent), title, message);
    }

    public static void warning(Component parent, String title, String message) {
        MessageDialog.showWarning(window(parent), title, message);
    }

    public static void error(Component parent, String title, String message) {
        MessageDialog.showError(window(parent), title, message);
    }

    /**
     * Reports a failure raised by the service layer.
     *
     * <p>{@link PawCareException#getDetailedMessage()} lists every validation problem the
     * record had, so the user sees the complete picture in a single dialog.</p>
     */
    public static void error(Component parent, String title, PawCareException failure) {
        MessageDialog.showError(window(parent), title, failure.getDetailedMessage());
    }

    /** A yes/no question. Returns true when the primary button was pressed. */
    public static boolean confirm(Component parent, String title, String message, String confirmLabel) {
        return MessageDialog.ask(window(parent), title, message, confirmLabel, "Cancel",
                MessageDialog.Kind.QUESTION);
    }

    /**
     * The standard delete confirmation, e.g. "Are you sure you want to delete Bruno?".
     *
     * @param subject the thing being deleted, already phrased as a noun phrase
     */
    public static boolean confirmDelete(Component parent, String subject) {
        return MessageDialog.ask(window(parent), "Delete confirmation",
                "Are you sure you want to delete " + subject + "?\n"
                        + "This action cannot be undone.",
                "Delete", "Cancel", MessageDialog.Kind.WARNING);
    }

    /**
     * The standard discard confirmation used when a form has unsaved changes.
     * Reserved for actions that would throw work away.
     */
    public static boolean confirmDiscard(Component parent, String subject) {
        return MessageDialog.ask(window(parent), "Discard changes?",
                "Any unsaved changes to " + subject + " will be lost.",
                "Discard", "Keep editing", MessageDialog.Kind.WARNING);
    }

    private static Window window(Component parent) {
        return MessageDialog.windowFor(parent);
    }
}
