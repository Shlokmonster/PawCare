package com.pawcare.gui.dialogs;

import com.pawcare.exception.PawCareException;
import com.pawcare.gui.components.ModernTextField;
import com.pawcare.model.Owner;
import com.pawcare.service.ClinicService;
import com.pawcare.util.IDGenerator;
import com.pawcare.util.ValidationUtil;

import java.awt.Window;

/**
 * The form used to add or edit a client.
 *
 * <p>It only collects input. Every rule – a real Indian mobile number, a well-formed
 * email, a name of at least two characters – lives in {@link ValidationUtil} and is
 * applied by the service, so the dialog cannot drift away from what the rest of the
 * application enforces.</p>
 */
public class OwnerDialog extends FormDialog {

    private final ClinicService clinic;
    private final Owner existing;

    private final ModernTextField nameField = new ModernTextField("e.g. Aarav Sharma");
    private final ModernTextField phoneField = new ModernTextField("e.g. 9876543210");
    private final ModernTextField emailField = new ModernTextField("e.g. aarav@example.com");
    private final ModernTextField addressField =
            new ModernTextField("House, street, area, city and PIN code");
    private final ModernTextField emergencyField =
            new ModernTextField("Optional second number");

    public OwnerDialog(Window owner, ClinicService clinic, Owner existing) {
        super(owner,
                existing == null ? "Add a new owner" : "Edit " + existing.getName(),
                existing == null
                        ? "Register a client so pets can be assigned to them."
                        : "Update this client's contact details.",
                existing == null ? "Add owner" : "Save changes");

        this.clinic = clinic;
        this.existing = existing;

        buildForm();
        populate();
    }

    private void buildForm() {
        form().section("Client details");
        form().row("Full name", nameField);
        form().row("Mobile number", phoneField);
        form().hint("A 10-digit Indian mobile number. A +91 prefix is accepted.");
        form().row("Email", emailField);
        form().row("Address", addressField);
        form().row("Emergency contact", emergencyField);
        form().hint("Optional. If given it must also be a valid mobile number.");
        form().finish();
    }

    private void populate() {
        if (existing == null) {
            return;
        }
        nameField.setText(existing.getName());
        phoneField.setText(existing.getPhone());
        emailField.setText(existing.getEmail());
        addressField.setText(existing.getAddress());
        emergencyField.setText(existing.getEmergencyContact());
    }

    @Override
    protected boolean onSave() throws PawCareException {
        clearErrors();

        String ownerId = existing == null ? IDGenerator.nextOwnerId() : existing.getOwnerId();

        // The service validates every rule and reports all failures at once.
        Owner owner = new Owner(ownerId, text(nameField), text(phoneField), text(emailField),
                text(addressField), text(emergencyField));

        if (existing == null) {
            clinic.addOwner(owner);
            toast(owner.getName() + " added as " + owner.getOwnerId());
        } else {
            clinic.updateOwner(owner);
            toast(owner.getName() + " updated");
        }
        return true;
    }
}
