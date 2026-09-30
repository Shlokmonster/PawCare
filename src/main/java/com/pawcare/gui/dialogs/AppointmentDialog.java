package com.pawcare.gui.dialogs;

import com.pawcare.exception.InvalidAppointmentException;
import com.pawcare.exception.PawCareException;
import com.pawcare.gui.components.ComboRenderers;
import com.pawcare.gui.components.ModernComboBox;
import com.pawcare.gui.components.ModernTextArea;
import com.pawcare.gui.components.ModernTextField;
import com.pawcare.model.Appointment;
import com.pawcare.model.Pet;
import com.pawcare.model.Veterinarian;
import com.pawcare.model.enums.AppointmentStatus;
import com.pawcare.service.ClinicService;
import com.pawcare.util.DateUtil;
import com.pawcare.util.IDGenerator;

import java.awt.Window;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Books or reschedules a clinic visit.
 *
 * <p>Besides the plain field checks it surfaces the scheduling rule the service enforces:
 * one veterinarian cannot hold two live appointments at the same date and time. The
 * service is the authority – the dialog only passes the message on.</p>
 */
public class AppointmentDialog extends FormDialog {

    private final ClinicService clinic;
    private final Appointment existing;

    private final ModernComboBox<Pet> petField;
    private final ModernComboBox<Veterinarian> vetField;
    private final ModernTextField dateField = new ModernTextField(DateUtil.DISPLAY_DATE_PATTERN);
    private final ModernTextField timeField = new ModernTextField(DateUtil.DISPLAY_TIME_PATTERN);
    private final ModernTextField reasonField = new ModernTextField("e.g. Annual wellness examination");
    private final ModernComboBox<AppointmentStatus> statusField =
            ModernComboBox.of(AppointmentStatus.class);
    private final ModernTextArea notesField =
            new ModernTextArea("Anything the clinic should prepare before the visit", 3);

    public AppointmentDialog(Window owner, ClinicService clinic, Appointment existing) {
        super(owner,
                existing == null ? "Book an appointment" : "Edit appointment " + existing.getAppointmentId(),
                existing == null
                        ? "Schedule a visit for one of the registered patients."
                        : "Change the booking details or its status.",
                existing == null ? "Book appointment" : "Save changes");

        this.clinic = clinic;
        this.existing = existing;

        this.petField = new ModernComboBox<>(clinic.pets().sortedByName());
        this.petField.setRenderer(ComboRenderers.pets());

        this.vetField = new ModernComboBox<>(clinic.veterinarians().sortedByName());

        buildForm();
        populate();
    }

    private void buildForm() {
        form().section("Booking");
        form().row("Patient", petField);
        form().row("Veterinarian", vetField);
        form().pair("Date & time", dateField, timeField);
        form().hint("Date " + DateUtil.DISPLAY_DATE_PATTERN + ", time " + DateUtil.DISPLAY_TIME_PATTERN
                + " (24-hour) — for example " + DateUtil.toEditable(LocalDate.now())
                + " and " + DateUtil.toEditable(LocalTime.of(14, 30)));
        form().row("Reason for visit", reasonField);

        form().section("Status");
        form().row("Status", statusField);
        form().hint("A scheduled appointment cannot be dated in the past.");
        form().stacked("Notes", notesField);

        if (clinic.pets().count() == 0) {
            form().hint("No pets are registered yet — register a pet before booking a visit.");
        }
        form().finish();
    }

    private void populate() {
        if (existing == null) {
            selectFirst(petField);
            selectFirst(vetField);
            dateField.setText(DateUtil.toEditable(LocalDate.now()));
            timeField.setText("10:00");
            statusField.setSelectedItem(AppointmentStatus.SCHEDULED);
            return;
        }

        selectPet(existing.getPetId());
        selectVet(existing.getVeterinarianId());
        dateField.setText(DateUtil.toEditable(existing.getAppointmentDate()));
        timeField.setText(DateUtil.toEditable(existing.getAppointmentTime()));
        reasonField.setText(existing.getReason());
        statusField.setSelectedItem(existing.getStatus());
        notesField.setText(existing.getNotes());
    }

    private void selectPet(String petId) {
        for (int i = 0; i < petField.getItemCount(); i++) {
            Pet candidate = petField.getItemAt(i);
            if (candidate != null && candidate.getAnimalId().equals(petId)) {
                petField.setSelectedIndex(i);
                return;
            }
        }
    }

    private void selectVet(String vetId) {
        for (int i = 0; i < vetField.getItemCount(); i++) {
            Veterinarian candidate = vetField.getItemAt(i);
            if (candidate != null && candidate.getVeterinarianId().equals(vetId)) {
                vetField.setSelectedIndex(i);
                return;
            }
        }
    }

    private static void selectFirst(ModernComboBox<?> combo) {
        if (combo.getItemCount() > 0) {
            combo.setSelectedIndex(0);
        }
    }

    @Override
    protected boolean onSave() throws PawCareException {
        clearErrors();

        List<String> problems = new ArrayList<>();

        LocalDate date = null;
        try {
            date = DateUtil.parseDate(dateField.getText());
        } catch (java.time.format.DateTimeParseException e) {
            problems.add("Date must look like " + DateUtil.DISPLAY_DATE_PATTERN + ".");
        }

        LocalTime time = null;
        try {
            time = DateUtil.parseTime(timeField.getText());
        } catch (java.time.format.DateTimeParseException e) {
            problems.add("Time must look like " + DateUtil.DISPLAY_TIME_PATTERN + " (24-hour), for example 14:30.");
        }

        Pet pet = (Pet) petField.getSelectedItem();
        if (pet == null) {
            problems.add("A patient must be selected.");
        }

        Veterinarian vet = (Veterinarian) vetField.getSelectedItem();
        if (vet == null) {
            problems.add("A veterinarian must be selected.");
        }

        if (!problems.isEmpty()) {
            throw new InvalidAppointmentException("Unable to save appointment.", problems);
        }

        AppointmentStatus status = (AppointmentStatus) statusField.getSelectedItem();
        String appointmentId = existing == null
                ? IDGenerator.nextAppointmentId() : existing.getAppointmentId();

        Appointment appointment = new Appointment(appointmentId, pet.getAnimalId(),
                vet.getVeterinarianId(), date, time, text(reasonField),
                status == null ? AppointmentStatus.SCHEDULED : status, text(notesField));

        if (existing == null) {
            clinic.addAppointment(appointment);
            toast("Appointment " + appointment.getAppointmentId() + " booked for " + pet.getName());
        } else {
            clinic.updateAppointment(appointment);
            toast("Appointment " + appointment.getAppointmentId() + " updated");
        }
        return true;
    }
}
