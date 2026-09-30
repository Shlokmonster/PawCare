package com.pawcare.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Small preference store backed by a {@code .properties} file.
 *
 * <p>The theme choice, the clinic name and the vaccination reminder window survive a
 * restart because they are written here. Using {@link Properties} rather than Java
 * serialization keeps the file human-readable and editable.</p>
 */
public class AppSettings {

    public static final String DEFAULT_CLINIC_NAME = "PawCare Veterinary Clinic";
    public static final int DEFAULT_REMINDER_DAYS = 30;

    private static final String KEY_THEME = "ui.theme";
    private static final String KEY_CLINIC_NAME = "clinic.name";
    private static final String KEY_REMINDER_DAYS = "reminder.window.days";

    private final Path file;
    private final Properties properties = new Properties();

    public AppSettings(Path file) {
        this.file = file;
        properties.setProperty(KEY_THEME, "light");
        properties.setProperty(KEY_CLINIC_NAME, DEFAULT_CLINIC_NAME);
        properties.setProperty(KEY_REMINDER_DAYS, String.valueOf(DEFAULT_REMINDER_DAYS));
    }

    /** Reads the file when it exists; a missing or unreadable file keeps the defaults. */
    public void load() {
        if (!Files.exists(file)) {
            return;
        }
        try (InputStream in = Files.newInputStream(file)) {
            properties.load(in);
        } catch (IOException e) {
            // Preferences are not critical: fall back to defaults and carry on.
            System.err.println("[PawCare] Could not read settings, using defaults: " + e.getMessage());
        }
    }

    public void save() {
        try {
            Path parent = file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (OutputStream out = Files.newOutputStream(file)) {
                properties.store(out, "PawCare preferences");
            }
        } catch (IOException e) {
            System.err.println("[PawCare] Could not write settings: " + e.getMessage());
        }
    }

    public String getTheme() {
        return properties.getProperty(KEY_THEME, "light");
    }

    public void setTheme(String theme) {
        properties.setProperty(KEY_THEME, theme);
    }

    public String getClinicName() {
        String value = properties.getProperty(KEY_CLINIC_NAME, DEFAULT_CLINIC_NAME);
        return value.isBlank() ? DEFAULT_CLINIC_NAME : value;
    }

    public void setClinicName(String clinicName) {
        properties.setProperty(KEY_CLINIC_NAME,
                ValidationUtil.isBlank(clinicName) ? DEFAULT_CLINIC_NAME : clinicName.trim());
    }

    public int getReminderDays() {
        try {
            int value = Integer.parseInt(properties.getProperty(KEY_REMINDER_DAYS, "30"));
            return value > 0 ? value : DEFAULT_REMINDER_DAYS;
        } catch (NumberFormatException e) {
            return DEFAULT_REMINDER_DAYS;
        }
    }

    public void setReminderDays(int days) {
        properties.setProperty(KEY_REMINDER_DAYS, String.valueOf(Math.max(1, days)));
    }
}
