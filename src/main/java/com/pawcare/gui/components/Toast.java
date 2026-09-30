package com.pawcare.gui.components;

import com.pawcare.theme.IconFactory;
import com.pawcare.theme.Theme;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;

/**
 * Brief confirmation messages that slide in at the bottom right of the window.
 *
 * <p>The toast is added straight to the window's layered pane, which avoids the flicker
 * and focus stealing of a small popup window and needs no translucency support from the
 * platform – the rounded shape is simply painted on an opaque background.</p>
 */
public final class Toast {

    /** The kind of message, which decides the icon and the accent colour. */
    public enum Type {
        SUCCESS("check"),
        ERROR("close"),
        INFO("info");

        private final String icon;

        Type(String icon) {
            this.icon = icon;
        }
    }

    private static final int VISIBLE_MILLIS = 2800;
    private static final int MARGIN = 34;

    private Toast() {
    }

    public static void success(JFrame frame, String message) {
        show(frame, message, Type.SUCCESS);
    }

    public static void error(JFrame frame, String message) {
        show(frame, message, Type.ERROR);
    }

    public static void info(JFrame frame, String message) {
        show(frame, message, Type.INFO);
    }

    /** Displays one toast, replacing any toast that is currently on screen. */
    public static void show(JFrame frame, String message, Type type) {
        if (frame == null || message == null || message.isBlank()) {
            return;
        }

        JLayeredPane layered = frame.getLayeredPane();

        // Only one toast at a time, so the corner never stacks up.
        for (Component component : layered.getComponents()) {
            if (component instanceof ToastPanel) {
                layered.remove(component);
            }
        }

        ToastPanel panel = new ToastPanel(message, type);
        layered.add(panel, JLayeredPane.POPUP_LAYER);

        Dimension size = panel.getPreferredSize();
        int x = Math.max(0, frame.getWidth() - size.width - MARGIN);
        int y = Math.max(0, frame.getHeight() - size.height - MARGIN);
        panel.setBounds(x, y, size.width, size.height);

        layered.repaint();

        Timer timer = new Timer(VISIBLE_MILLIS, e -> {
            layered.remove(panel);
            layered.repaint();
        });
        timer.setRepeats(false);
        timer.start();
    }

    /** The message surface itself. */
    private static final class ToastPanel extends RoundedPanel {

        ToastPanel(String message, Type type) {
            super(new BorderLayout(Theme.SPACE_MD, 0));
            setRadius(12);
            setCardBackground(Theme.c().surface);
            setBorderColor(Theme.c().border);
            setBorder(BorderFactory.createEmptyBorder(
                    Theme.SPACE_MD, Theme.SPACE_LG, Theme.SPACE_MD, Theme.SPACE_LG));

            Color accent = accentFor(type);

            JLabel icon = new JLabel(IconFactory.of(type.icon, 16, accent));
            add(icon, BorderLayout.WEST);

            JLabel text = new JLabel(message);
            text.setFont(Theme.FONT_BODY_MEDIUM);
            text.setForeground(Theme.c().textPrimary);
            add(text, BorderLayout.CENTER);

            FontMetrics metrics = getFontMetrics(Theme.FONT_BODY_MEDIUM);
            int width = metrics.stringWidth(message) + 96;
            setPreferredSize(new Dimension(Math.min(430, width), 52));
        }

        private static Color accentFor(Type type) {
            switch (type) {
                case SUCCESS:
                    return Theme.c().success;
                case ERROR:
                    return Theme.c().danger;
                default:
                    return Theme.c().primary;
            }
        }
    }
}
