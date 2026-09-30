package com.pawcare.gui.components;

import com.pawcare.theme.IconFactory;
import com.pawcare.theme.Theme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;

/**
 * The panel shown when a list or table has nothing to display.
 *
 * <p>An explicit empty state tells the user whether the clinic genuinely has no records
 * or whether a filter simply matched nothing – far more useful than a blank area.</p>
 */
public class EmptyStatePanel extends JPanel {

    private final JLabel titleLabel = new JLabel();
    private final JLabel messageLabel = new JLabel();
    private final JLabel iconLabel = new JLabel();
    private final JPanel actionHolder = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));

    public EmptyStatePanel(String iconName, String title, String message) {
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        iconLabel.setIcon(IconFactory.of(iconName, 36, Theme.c().textMuted));
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        titleLabel.setText(title);
        titleLabel.setFont(Theme.FONT_H3);
        titleLabel.setForeground(Theme.c().textPrimary);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        messageLabel.setText(message);
        messageLabel.setFont(Theme.FONT_SMALL);
        messageLabel.setForeground(Theme.c().textSecondary);
        messageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        actionHolder.setOpaque(false);
        actionHolder.setAlignmentX(Component.CENTER_ALIGNMENT);

        add(Box.createVerticalGlue());
        add(iconLabel);
        add(Box.createVerticalStrut(Theme.SPACE_MD));
        add(titleLabel);
        add(Box.createVerticalStrut(Theme.SPACE_XS));
        add(messageLabel);
        add(Box.createVerticalStrut(Theme.SPACE_MD));
        add(actionHolder);
        add(Box.createVerticalGlue());

        setBorder(BorderFactory.createEmptyBorder(
                Theme.SPACE_XL, Theme.SPACE_XL, Theme.SPACE_XL, Theme.SPACE_XL));
    }

    /** Adds a call-to-action button, for example "Add your first pet". */
    public EmptyStatePanel withAction(Component action) {
        actionHolder.removeAll();
        actionHolder.add(action);
        return this;
    }

    public void setText(String title, String message) {
        titleLabel.setText(title);
        messageLabel.setText(message);
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        return new Dimension(Math.max(size.width, 260), Math.max(size.height, 180));
    }
}
