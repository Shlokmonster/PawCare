package com.pawcare.theme;

import java.awt.Color;

/** Small colour arithmetic helpers used by the custom-painted components. */
public final class Colors {

    private Colors() {
    }

    /** Makes a colour darker by the given fraction (0.0 – 1.0). */
    public static Color darken(Color color, double amount) {
        if (color == null) {
            return null;
        }
        double factor = Math.max(0, Math.min(1, 1 - amount));
        return new Color(
                (int) Math.round(color.getRed() * factor),
                (int) Math.round(color.getGreen() * factor),
                (int) Math.round(color.getBlue() * factor),
                color.getAlpha());
    }

    /** Makes a colour lighter by blending it towards white. */
    public static Color lighten(Color color, double amount) {
        if (color == null) {
            return null;
        }
        double a = Math.max(0, Math.min(1, amount));
        return new Color(
                (int) Math.round(color.getRed() + (255 - color.getRed()) * a),
                (int) Math.round(color.getGreen() + (255 - color.getGreen()) * a),
                (int) Math.round(color.getBlue() + (255 - color.getBlue()) * a),
                color.getAlpha());
    }

    /** Same colour with a different alpha value (0 – 255). */
    public static Color alpha(Color color, int alpha) {
        if (color == null) {
            return null;
        }
        return new Color(color.getRed(), color.getGreen(), color.getBlue(),
                Math.max(0, Math.min(255, alpha)));
    }

    /** Mixes two colours, weight being how much of {@code other} is used. */
    public static Color mix(Color base, Color other, double weight) {
        double w = Math.max(0, Math.min(1, weight));
        return new Color(
                (int) Math.round(base.getRed() * (1 - w) + other.getRed() * w),
                (int) Math.round(base.getGreen() * (1 - w) + other.getGreen() * w),
                (int) Math.round(base.getBlue() * (1 - w) + other.getBlue() * w));
    }
}
