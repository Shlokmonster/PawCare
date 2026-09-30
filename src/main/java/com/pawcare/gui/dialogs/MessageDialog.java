package com.pawcare.gui.dialogs;

import com.pawcare.gui.components.ModernButton;
import com.pawcare.gui.components.RoundedPanel;
import com.pawcare.theme.Colors;
import com.pawcare.theme.IconFactory;
import com.pawcare.theme.Theme;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * The single styled dialog used for confirmations, warnings and error reports.
 *
 * <p>It replaces {@code JOptionPane} so that messages match the rest of the interface and
 * can show a multi-line list of validation problems. The window is undecorated and
 * repainted by hand; when the platform supports it the window itself is made translucent
 * so the rounded corners and the drop shadow are visible.</p>
 */
public class MessageDialog extends JDialog {

    /** Decides the icon, the accent colour and the default button style. */
    public enum Kind {
        INFO("info"),
        SUCCESS("check"),
        WARNING("bell"),
        ERROR("close"),
        QUESTION("info");

        private final String icon;

        Kind(String icon) {
            this.icon = icon;
        }
    }

    private boolean confirmed;
    private Point dragOrigin;

    private MessageDialog(Window owner, String title, String message, Kind kind,
                          String confirmText, String cancelText) {
        super(owner, ModalityType.APPLICATION_MODAL);
        setUndecorated(true);

        // Per-pixel translucency is not available everywhere; when it is missing the
        // dialog simply keeps its opaque window background, which is harmless.
        try {
            setBackground(new Color(0, 0, 0, 0));
        } catch (UnsupportedOperationException ignored) {
            // Translucency unsupported – the square corners are an acceptable fallback.
        }

        Color accent = accentFor(kind);

        RoundedPanel card = new RoundedPanel(new BorderLayout(0, Theme.SPACE_LG));
        card.setRadius(16);
        card.setCardBackground(Theme.c().surface);
        card.setBorderColor(Theme.c().border);
        card.setShadow(true);
        card.setBorder(BorderFactory.createEmptyBorder(
                Theme.SPACE_XL, Theme.SPACE_XL, Theme.SPACE_XL, Theme.SPACE_XL));

        card.add(buildHeader(title, kind, accent), BorderLayout.NORTH);
        card.add(buildMessage(message), BorderLayout.CENTER);
        card.add(buildButtons(confirmText, cancelText, kind), BorderLayout.SOUTH);

        setContentPane(card);
        getRootPane().setOpaque(false);

        // Frameless windows have no title bar, so dragging is wired to the header.
        installDragSupport(card);

        // Escape always cancels, and Enter activates the primary button.
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        pack();
        setLocationRelativeTo(owner);
    }

    // ------------------------------------------------------------------
    // Building blocks
    // ------------------------------------------------------------------

    private JPanel buildHeader(String title, Kind kind, Color accent) {
        JPanel header = new JPanel(new BorderLayout(Theme.SPACE_MD, 0));
        header.setOpaque(false);

        RoundedPanel tile = new RoundedPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        tile.setRadius(11);
        tile.setCardBackground(Colors.alpha(accent, 36));
        tile.setPreferredSize(new Dimension(40, 40));

        JLabel icon = new JLabel(IconFactory.of(kind.icon, 19, accent));
        icon.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        tile.add(icon);

        JPanel tileHolder = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        tileHolder.setOpaque(false);
        tileHolder.add(tile);
        header.add(tileHolder, BorderLayout.WEST);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(Theme.FONT_H2);
        titleLabel.setForeground(Theme.c().textPrimary);
        header.add(titleLabel, BorderLayout.CENTER);

        return header;
    }

    private JComponent buildMessage(String message) {
        JTextArea area = new JTextArea(message == null ? "" : message);
        area.setEditable(false);
        area.setFocusable(false);
        area.setOpaque(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(Theme.FONT_BODY);
        area.setForeground(Theme.c().textSecondary);
        area.setBorder(BorderFactory.createEmptyBorder());

        // The width is measured from the text so short messages stay compact and long
        // ones wrap onto several lines instead of stretching the dialog off screen.
        FontMetrics metrics = area.getFontMetrics(Theme.FONT_BODY);
        int longest = 0;
        for (String line : (message == null ? "" : message).split("\n")) {
            longest = Math.max(longest, metrics.stringWidth(line));
        }
        int width = Math.min(430, Math.max(280, longest + 8));
        area.setSize(new Dimension(width, Short.MAX_VALUE));
        area.setPreferredSize(new Dimension(width, area.getPreferredSize().height));

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(area, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel buildButtons(String confirmText, String cancelText, Kind kind) {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, Theme.SPACE_SM, 0));
        footer.setOpaque(false);

        if (cancelText != null) {
            ModernButton cancel = new ModernButton(cancelText, ModernButton.Variant.SECONDARY);
            cancel.addActionListener(e -> {
                confirmed = false;
                dispose();
            });
            footer.add(cancel);
        }

        ModernButton confirm = new ModernButton(confirmText,
                kind == Kind.ERROR || kind == Kind.WARNING
                        ? ModernButton.Variant.DANGER
                        : ModernButton.Variant.PRIMARY);
        confirm.addActionListener(e -> {
            confirmed = true;
            dispose();
        });
        footer.add(confirm);

        getRootPane().setDefaultButton(confirm);
        return footer;
    }

    private void installDragSupport(JComponent header) {
        MouseAdapter adapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                dragOrigin = e.getPoint();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (dragOrigin == null) {
                    return;
                }
                Point location = getLocation();
                setLocation(location.x + e.getX() - dragOrigin.x,
                        location.y + e.getY() - dragOrigin.y);
            }
        };
        header.addMouseListener(adapter);
        header.addMouseMotionListener(adapter);
    }

    private static Color accentFor(Kind kind) {
        switch (kind) {
            case SUCCESS:
                return Theme.c().success;
            case WARNING:
                return Theme.c().warning;
            case ERROR:
                return Theme.c().danger;
            default:
                return Theme.c().primary;
        }
    }

    /** True when the user pressed the primary button. */
    public boolean isConfirmed() {
        return confirmed;
    }

    // ------------------------------------------------------------------
    // Static entry points
    // ------------------------------------------------------------------

    public static void showInfo(Window owner, String title, String message) {
        new MessageDialog(owner, title, message, Kind.INFO, "Close", null).setVisible(true);
    }

    public static void showSuccess(Window owner, String title, String message) {
        new MessageDialog(owner, title, message, Kind.SUCCESS, "Close", null).setVisible(true);
    }

    public static void showError(Window owner, String title, String message) {
        new MessageDialog(owner, title, message, Kind.ERROR, "Close", null).setVisible(true);
    }

    public static void showWarning(Window owner, String title, String message) {
        new MessageDialog(owner, title, message, Kind.WARNING, "Close", null).setVisible(true);
    }

    /** Asks a yes/no question; returns true when the primary button was pressed. */
    public static boolean ask(Window owner, String title, String message,
                              String confirmText, String cancelText, Kind kind) {
        MessageDialog dialog = new MessageDialog(owner, title, message, kind, confirmText, cancelText);
        dialog.setVisible(true);
        return dialog.isConfirmed();
    }

    /** Window lookup helper so callers can pass any component. */
    public static Window windowFor(java.awt.Component parent) {
        if (parent == null) {
            return null;
        }
        if (parent instanceof Window) {
            return (Window) parent;
        }
        Window window = SwingUtilities.getWindowAncestor(parent);
        if (window != null) {
            return window;
        }
        // Fall back to the first frame, which is the main window in this application.
        Frame[] frames = Frame.getFrames();
        return frames.length > 0 ? frames[0] : null;
    }

    /** Convenience used by the tests and by callers that only need a yes/no answer. */
    public static boolean confirm(Window owner, String message) {
        return ask(owner, "Please confirm", message, "Confirm", "Cancel", Kind.QUESTION);
    }
}
