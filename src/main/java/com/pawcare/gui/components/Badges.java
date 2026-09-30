package com.pawcare.gui.components;

import com.pawcare.model.enums.AppointmentStatus;
import com.pawcare.model.enums.PetHealthStatus;
import com.pawcare.model.enums.VaccinationStatus;
import com.pawcare.theme.Theme;

import java.awt.Color;

/**
 * Maps a status value to the text and colours of a badge.
 *
 * <p>Kept separate from {@link BadgeRenderer} so the same rules can be used by tables, by
 * the dashboard lists and by the reminder panel – anywhere a status needs to look the
 * same.</p>
 */
public final class Badges {

    /** The resolved appearance of one badge. */
    public static final class Spec {

        public final String text;
        public final Color foreground;
        public final Color background;

        Spec(String text, Color foreground, Color background) {
            this.text = text;
            this.foreground = foreground;
            this.background = background;
        }
    }

    private Badges() {
    }

    public static Spec resolve(Object value) {
        if (value instanceof AppointmentStatus) {
            AppointmentStatus status = (AppointmentStatus) value;
            return new Spec(status.getLabel(), Theme.badgeText(status), Theme.badgeBackground(status));
        }
        if (value instanceof VaccinationStatus) {
            VaccinationStatus status = (VaccinationStatus) value;
            return new Spec(status.getLabel(), Theme.badgeText(status), Theme.badgeBackground(status));
        }
        if (value instanceof PetHealthStatus) {
            PetHealthStatus status = (PetHealthStatus) value;
            switch (status) {
                case OVERDUE:
                    return new Spec(status.getLabel(), Theme.c().danger, Theme.c().dangerSoft);
                case DUE_SOON:
                    return new Spec(status.getLabel(), Theme.c().warning, Theme.c().warningSoft);
                case UP_TO_DATE:
                    return new Spec(status.getLabel(), Theme.c().success, Theme.c().successSoft);
                default:
                    return new Spec(status.getLabel(), Theme.c().textSecondary, Theme.c().surfaceAlt);
            }
        }
        if (value == null || String.valueOf(value).isBlank()) {
            return new Spec("—", Theme.c().textMuted, Theme.c().surfaceAlt);
        }
        return new Spec(String.valueOf(value), Theme.c().textSecondary, Theme.c().primarySoft);
    }

    /** Builds a ready-to-use badge for the given value. */
    public static StatusBadge badge(Object value) {
        Spec spec = resolve(value);
        return new StatusBadge(spec.text, spec.foreground, spec.background);
    }
}
