package com.pawcare.model;

import java.io.Serializable;
import java.util.Objects;

/** A doctor employed by the clinic. */
public class Veterinarian implements Serializable {

    private static final long serialVersionUID = 1L;

    private String veterinarianId;
    private String name;
    private String specialization;
    private String phone;
    private String email;

    public Veterinarian() {
    }

    public Veterinarian(String veterinarianId, String name, String specialization,
                        String phone, String email) {
        this.veterinarianId = veterinarianId;
        this.name = name;
        this.specialization = specialization;
        this.phone = phone;
        this.email = email;
    }

    public String displayInfo() {
        return name + " • " + specialization;
    }

    public String getVeterinarianId() {
        return veterinarianId;
    }

    public void setVeterinarianId(String veterinarianId) {
        this.veterinarianId = veterinarianId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSpecialization() {
        return specialization;
    }

    public void setSpecialization(String specialization) {
        this.specialization = specialization;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Veterinarian)) {
            return false;
        }
        return Objects.equals(veterinarianId, ((Veterinarian) other).veterinarianId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(veterinarianId);
    }

    /** Combobox-friendly label, e.g. "Dr. Meera Nair (Surgery)". */
    @Override
    public String toString() {
        return name + " (" + specialization + ")";
    }
}
