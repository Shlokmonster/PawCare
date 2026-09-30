package com.pawcare.model;

import com.pawcare.model.enums.AppointmentStatus;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

/**
 * A booked visit. Appointments are grouped by date inside a
 * {@code TreeMap<LocalDate, ArrayList<Appointment>>} held by the service layer, which
 * keeps the schedule sorted automatically.
 */
public class Appointment implements Serializable {

    private static final long serialVersionUID = 1L;

    private String appointmentId;
    private String petId;
    private String veterinarianId;
    private LocalDate appointmentDate;
    private LocalTime appointmentTime;
    private String reason;
    private AppointmentStatus status;
    private String notes;

    public Appointment() {
        this.status = AppointmentStatus.SCHEDULED;
    }

    public Appointment(String appointmentId, String petId, String veterinarianId,
                       LocalDate appointmentDate, LocalTime appointmentTime,
                       String reason, AppointmentStatus status, String notes) {
        this.appointmentId = appointmentId;
        this.petId = petId;
        this.veterinarianId = veterinarianId;
        this.appointmentDate = appointmentDate;
        this.appointmentTime = appointmentTime;
        this.reason = reason;
        this.status = status == null ? AppointmentStatus.SCHEDULED : status;
        this.notes = notes;
    }

    public String displayInfo() {
        return appointmentId + " • " + appointmentDate + " " + appointmentTime + " • " + reason;
    }

    public String getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(String appointmentId) {
        this.appointmentId = appointmentId;
    }

    public String getPetId() {
        return petId;
    }

    public void setPetId(String petId) {
        this.petId = petId;
    }

    public String getVeterinarianId() {
        return veterinarianId;
    }

    public void setVeterinarianId(String veterinarianId) {
        this.veterinarianId = veterinarianId;
    }

    public LocalDate getAppointmentDate() {
        return appointmentDate;
    }

    public void setAppointmentDate(LocalDate appointmentDate) {
        this.appointmentDate = appointmentDate;
    }

    public LocalTime getAppointmentTime() {
        return appointmentTime;
    }

    public void setAppointmentTime(LocalTime appointmentTime) {
        this.appointmentTime = appointmentTime;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public AppointmentStatus getStatus() {
        return status;
    }

    public void setStatus(AppointmentStatus status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Appointment)) {
            return false;
        }
        return Objects.equals(appointmentId, ((Appointment) other).appointmentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(appointmentId);
    }

    @Override
    public String toString() {
        return appointmentId;
    }
}
