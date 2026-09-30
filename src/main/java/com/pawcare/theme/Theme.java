package com.pawcare.theme;

import com.pawcare.model.enums.AppointmentStatus;
import com.pawcare.model.enums.VaccinationStatus;

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Central design system: colours, fonts, spacing and corner radii.
 *
 * <p>No component in the application is allowed to hard-code a colour or a magic
 * number. Everything is read from here, so the look stays consistent and a change such
 * as "make the accent teal instead of green" is a single edit.</p>
 */
public final class Theme {

    // ------------------------------------------------------------------
    // Active palette
    // ------------------------------------------------------------------

    private static Palette current = Palette.light();

    /** Short accessor: {@code Theme.c().primary} reads better than {@code Theme.getColors()...}. */
    public static Palette c() {
        return current;
    }

    /** Swaps the active palette. Called by {@link ThemeManager}. */
    static void use(Palette palette) {
        current = palette;
    }

    // ------------------------------------------------------------------
    // Spacing scale (4-pixel rhythm)
    // ------------------------------------------------------------------

    public static final int SPACE_XS = 4;
    public static final int SPACE_SM = 8;
    public static final int SPACE_MD = 12;
    public static final int SPACE_LG = 16;
    public static final int SPACE_XL = 24;
    public static final int SPACE_2XL = 32;

    /** Corner radius used by cards and panels. */
    public static final int RADIUS_CARD = 14;
    /** Corner radius used by buttons and small chips. */
    public static final int RADIUS_CONTROL = 9;

    public static final int SIDEBAR_WIDTH = 236;
    public static final int TABLE_ROW_HEIGHT = 42;
    public static final int CONTROL_HEIGHT = 36;

    // ------------------------------------------------------------------
    // Typography
    // ------------------------------------------------------------------

    private static final String FAMILY = resolveFamily();

    private static String resolveFamily() {
        String[] candidates = {
                "SF Pro Text", "Helvetica Neue", "Segoe UI", "Inter",
                "Roboto", "Noto Sans", "DejaVu Sans", "Arial", Font.SANS_SERIF
        };
        Set<String> available = new HashSet<>(Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
        for (String candidate : candidates) {
            if (available.contains(candidate)) {
                return candidate;
            }
        }
        return Font.SANS_SERIF;
    }

    public static Font font(int size, int style) {
        return new Font(FAMILY, style, size);
    }

    public static final Font FONT_DISPLAY = font(28, Font.BOLD);   // big dashboard numbers
    public static final Font FONT_H1 = font(21, Font.BOLD);        // page title
    public static final Font FONT_H2 = font(16, Font.BOLD);        // card title
    public static final Font FONT_H3 = font(13, Font.BOLD);        // small heading
    public static final Font FONT_BODY = font(13, Font.PLAIN);
    public static final Font FONT_BODY_MEDIUM = font(13, Font.BOLD);
    public static final Font FONT_SMALL = font(12, Font.PLAIN);
    public static final Font FONT_SMALL_MEDIUM = font(12, Font.BOLD);
    public static final Font FONT_TINY = font(11, Font.PLAIN);
    public static final Font FONT_BUTTON = font(13, Font.BOLD);
    public static final Font FONT_LOGO = font(20, Font.BOLD);

    // ------------------------------------------------------------------
    // Semantic colour helpers
    // ------------------------------------------------------------------

    /** Badge colour for a vaccination reminder state. */
    public static Color badgeText(VaccinationStatus status) {
        switch (status) {
            case OVERDUE:
                return c().danger;
            case DUE_SOON:
                return c().warning;
            default:
                return c().success;
        }
    }

    /** Soft badge background for a vaccination reminder state. */
    public static Color badgeBackground(VaccinationStatus status) {
        switch (status) {
            case OVERDUE:
                return c().dangerSoft;
            case DUE_SOON:
                return c().warningSoft;
            default:
                return c().successSoft;
        }
    }

    /** Badge colour for an appointment state. */
    public static Color badgeText(AppointmentStatus status) {
        switch (status) {
            case COMPLETED:
                return c().success;
            case CANCELLED:
                return c().danger;
            default:
                return c().info;
        }
    }

    /** Soft badge background for an appointment state. */
    public static Color badgeBackground(AppointmentStatus status) {
        switch (status) {
            case COMPLETED:
                return c().successSoft;
            case CANCELLED:
                return c().dangerSoft;
            default:
                return c().infoSoft;
        }
    }

    /** Muted text used for the secondary line inside list rows. */
    public static Color subtleText() {
        return c().textSecondary;
    }

    private Theme() {
    }
}
