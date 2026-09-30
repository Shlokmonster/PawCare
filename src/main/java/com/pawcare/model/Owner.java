package com.pawcare.model;

import java.io.Serializable;
import java.util.Objects;

/** A client of the clinic – the person who owns one or more pets. */
public class Owner implements Serializable {

    private static final long serialVersionUID = 1L;

    private String ownerId;
    private String name;
    private String phone;
    private String email;
    private String address;
    private String emergencyContact;

    public Owner() {
    }

    public Owner(String ownerId, String name, String phone, String email,
                 String address, String emergencyContact) {
        this.ownerId = ownerId;
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.address = address;
        this.emergencyContact = emergencyContact;
    }

    public String displayInfo() {
        return name + " • " + phone + " • " + email;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getEmergencyContact() {
        return emergencyContact;
    }

    public void setEmergencyContact(String emergencyContact) {
        this.emergencyContact = emergencyContact;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Owner)) {
            return false;
        }
        return Objects.equals(ownerId, ((Owner) other).ownerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ownerId);
    }

    @Override
    public String toString() {
        return name == null ? ownerId : name;
    }
}
