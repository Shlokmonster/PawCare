package com.pawcare.gui.dialogs;

import com.pawcare.exception.PawCareException;
import com.pawcare.gui.components.FormPanel;
import com.pawcare.gui.components.ModernButton;
import com.pawcare.gui.components.RoundedPanel;
import com.pawcare.gui.components.ScrollPanes;
import com.pawcare.gui.components.Toast;
import com.pawcare.theme.Theme;
import com.pawcare.util.ValidationUtil;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Shared shell for every "add / edit" dialog.
 *
 * <p>Each concrete dialog only has to declare its fields in the constructor and implement
 * {@link #onSave()}. Everything else – the framed window, the title block, the scrolling
 * form area, the inline error box and the Cancel/Save footer – is built here once, so all
 * five forms in the application look and behave identically.</p>
 *
 * <p>Validation failures are shown <i>inside</i> the dialog rather than in a second
 * pop-up. That keeps the offending form on screen while the user reads the problem, which
 * is what {@link PawCareException#getDetailedMessage()} exists for.</p>
 */
public abstract class FormDialog extends JDialog {

    /** Forms taller than this start to scroll instead of growing further. */
    private static final int MAX_FORM_HEIGHT = 470;
    /** Width of the dialog body. */
    private static final int FORM_WIDTH = 520;

    private final FormPanel form = new FormPanel();

    private final RoundedPanel errorPanel = new RoundedPanel(new BorderLayout(Theme.SPACE_SM, 0));
    private final JTextArea errorText = new JTextArea();

    private final ModernButton saveButton;
    private boolean saved;
    private Point dragOrigin;

    protected FormDialog(Window owner, String title, String subtitle, String saveLabel) {
        super(owner, ModalityType.APPLICATION_MODAL);
        setUndecorated(true);

        // Rounded corners and the drop shadow need a translucent window. The platform does
        // not always allow it, so this is best-effort.
        try {
            setBackground(new Color(0, 0, 0, 0));
        } catch (UnsupportedOperationException ignored) {
            // Falls back to square corners on the affected platforms.
        }

        RoundedPanel card = new RoundedPanel(new BorderLayout(0, 0));
        card.setRadius(16);
        card.setCardBackground(Theme.c().surface);
        card.setBorderColor(Theme.c().border);
        card.setShadow(true);
        card.setBorder(BorderFactory.createEmptyBorder(
                Theme.SPACE_XL, Theme.SPACE_XL, Theme.SPACE_XL, Theme.SPACE_XL));

        JPanel north = new JPanel();
        north.setOpaque(false);
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        north.add(buildHeader(title, subtitle));
        north.add(Box.createVerticalStrut(Theme.SPACE_LG));
        north.add(buildErrorPanel());
        card.add(north, BorderLayout.NORTH);

        JScrollPane scroll = ScrollPanes.create(form);
        scroll.getViewport().setBackground(Theme.c().surface);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        card.add(scroll, BorderLayout.CENTER);

        saveButton = new ModernButton(saveLabel, ModernButton.Variant.PRIMARY, "check");
        card.add(buildFooter(), BorderLayout.SOUTH);

        setContentPane(card);
        getRootPane().setOpaque(false);
        getRootPane().setDefaultButton(saveButton);

        installDragSupport(card);
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    // ------------------------------------------------------------------
    // API for the concrete dialogs
    // ------------------------------------------------------------------

    /** The form the subclass adds its rows to. */
    protected FormPanel form() {
        return form;
    }

    /** True when the record was saved, so the caller knows whether to refresh. */
    public boolean isSaved() {
        return saved;
    }

    /**
     * Reads the fields and saves the record.
     *
     * @return false to abort without closing (rarely needed)
     * @throws PawCareException when the input is invalid; the dialog stays open and
     *                          displays every problem at once
     */
    protected abstract boolean onSave() throws PawCareException;

    /** The frame this dialog's toast should appear on, or null when there is none. */
    protected JFrame mainFrame() {
        Window owner = getOwner();
        return owner instanceof JFrame ? (JFrame) owner : null;
    }

    /** Pops a success toast on the main window. */
    protected void toast(String message) {
        JFrame frame = mainFrame();
        if (frame != null) {
            Toast.success(frame, message);
        }
    }

    // ------------------------------------------------------------------
    // Layout
    // ------------------------------------------------------------------

    private JPanel buildHeader(String title, String subtitle) {
        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(Theme.FONT_H1);
        titleLabel.setForeground(Theme.c().textPrimary);
        titleLabel.setAlignmentX(LEFT_ALIGNMENT);
        header.add(titleLabel);

        if (subtitle != null && !subtitle.isBlank()) {
            header.add(Box.createVerticalStrut(Theme.SPACE_XS));
            JLabel subtitleLabel = new JLabel(subtitle);
            subtitleLabel.setFont(Theme.FONT_SMALL);
            subtitleLabel.setForeground(Theme.c().textSecondary);
            subtitleLabel.setAlignmentX(LEFT_ALIGNMENT);
            header.add(subtitleLabel);
        }
        return header;
    }

    /** The inline error box, hidden until {@link #showErrors} is called. */
    private JComponent buildErrorPanel() {
        errorPanel.setRadius(Theme.RADIUS_CONTROL);
        errorPanel.setCardBackground(Theme.c().dangerSoft);
        errorPanel.setBorderColor(Theme.c().danger);
        errorPanel.setBorder(BorderFactory.createEmptyBorder(
                Theme.SPACE_MD, Theme.SPACE_MD, Theme.SPACE_MD, Theme.SPACE_MD));
        errorPanel.setVisible(false);

        errorText.setEditable(false);
        errorText.setFocusable(false);
        errorText.setOpaque(false);
        errorText.setLineWrap(true);
        errorText.setWrapStyleWord(true);
        errorText.setFont(Theme.FONT_SMALL);
        errorText.setForeground(Theme.c().danger);
        errorText.setBorder(BorderFactory.createEmptyBorder());

        errorPanel.add(errorText, BorderLayout.CENTER);
        return errorPanel;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createEmptyBorder(Theme.SPACE_LG, 0, 0, 0));

        JLabel hint = new JLabel("Press Esc to cancel");
        hint.setFont(Theme.FONT_TINY);
        hint.setForeground(Theme.c().textMuted);
        footer.add(hint, BorderLayout.WEST);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, Theme.SPACE_SM, 0));
        buttons.setOpaque(false);

        ModernButton cancel = new ModernButton("Cancel", ModernButton.Variant.SECONDARY);
        cancel.addActionListener(e -> dispose());
        buttons.add(cancel);

        saveButton.addActionListener(e -> attemptSave());
        buttons.add(saveButton);

        footer.add(buttons, BorderLayout.EAST);
        return footer;
    }

    private void installDragSupport(JComponent surface) {
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
        surface.addMouseListener(adapter);
        surface.addMouseMotionListener(adapter);
    }

    // ------------------------------------------------------------------
    // Saving
    // ------------------------------------------------------------------

    private void attemptSave() {
        try {
            if (onSave()) {
                saved = true;
                dispose();
            }
        } catch (PawCareException e) {
            // Every problem the record has is listed, not just the first one.
            showErrors(e.getDetailedMessage());
        }
    }

    /** Displays a validation message inside the dialog. */
    protected void showErrors(String message) {
        errorText.setText(message);
        errorText.setSize(new Dimension(FORM_WIDTH - 40, Short.MAX_VALUE));
        errorPanel.setVisible(true);
        revalidate();
        packKeepingPosition();
    }

    /** Clears the error box, for instance after the user corrects a field. */
    protected void clearErrors() {
        errorText.setText("");
        errorPanel.setVisible(false);
        revalidate();
    }

    // ------------------------------------------------------------------
    // Showing
    // ------------------------------------------------------------------

    /**
     * Sizes the dialog once the subclass has finished adding its fields.
     *
     * <p>Overriding {@code setVisible} rather than calling {@code pack()} from the
     * constructor means a subclass can add rows after {@code super(...)} has returned,
     * which is the natural way to write these dialogs.</p>
     */
    @Override
    public void setVisible(boolean visible) {
        if (visible && !isDisplayable()) {
            prepare();
        }
        super.setVisible(visible);
    }

    private void prepare() {
        // The form must be laid out at its intended width before its height can be
        // measured, otherwise the scroll pane would size itself from a stale value.
        form.setPreferredSize(null);
        form.setSize(new Dimension(FORM_WIDTH, Short.MAX_VALUE));

        Dimension preferred = form.getPreferredSize();
        int height = Math.min(preferred.height, MAX_FORM_HEIGHT);
        form.setPreferredSize(new Dimension(FORM_WIDTH, height));
        form.setMinimumSize(new Dimension(FORM_WIDTH, height));

        pack();
        setLocationRelativeTo(getOwner());
        SwingUtilities.invokeLater(this::focusFirstField);
    }

    /** Focuses the first text field so the user can start typing immediately. */
    private void focusFirstField() {
        for (java.awt.Component component : form.getComponents()) {
            if (component instanceof JComponent) {
                JComponent candidate = findTextField((JComponent) component);
                if (candidate != null) {
                    candidate.requestFocusInWindow();
                    return;
                }
            }
        }
    }

    private JComponent findTextField(JComponent component) {
        if (component instanceof javax.swing.text.JTextComponent) {
            return component;
        }
        if (component instanceof java.awt.Container) {
            for (java.awt.Component child : ((java.awt.Container) component).getComponents()) {
                if (child instanceof JComponent) {
                    JComponent found = findTextField((JComponent) child);
                    if (found != null) {
                        return found;
                    }
                }
            }
        }
        return null;
    }

    private void packKeepingPosition() {
        int x = getX();
        int y = getY();
        pack();
        setLocation(x, y);
    }

    // ------------------------------------------------------------------
    // Small helpers shared by the concrete dialogs
    // ------------------------------------------------------------------

    /** A muted hint line shown under a field, e.g. the accepted date format. */
    protected static JLabel hint(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.FONT_TINY);
        label.setForeground(Theme.c().textMuted);
        return label;
    }

    /** Trims a field, turning null into the empty string. */
    protected static String text(javax.swing.text.JTextComponent field) {
        return ValidationUtil.clean(field.getText());
    }
}
