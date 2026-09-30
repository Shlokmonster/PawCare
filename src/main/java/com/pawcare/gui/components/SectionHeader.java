package com.pawcare.gui.components;

import com.pawcare.theme.Theme;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;

/**
 * A small heading used to separate sections inside a page, for example above a table or
 * above the reminder list.
 */
public class SectionHeader extends JPanel {

    private final JLabel titleLabel = new JLabel();
    private final JLabel subtitleLabel = new JLabel();
    private final JPanel trailing = new JPanel(new FlowLayout(FlowLayout.RIGHT, Theme.SPACE_SM, 0));

    public SectionHeader(String title) {
        this(title, null);
    }

    public SectionHeader(String title, String subtitle) {
        setOpaque(false);
        setLayout(new BorderLayout(Theme.SPACE_MD, 0));

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));

        titleLabel.setText(title);
        titleLabel.setFont(Theme.FONT_H3);
        titleLabel.setForeground(Theme.c().textPrimary);
        titleLabel.setAlignmentX(LEFT_ALIGNMENT);
        text.add(titleLabel);

        if (subtitle != null && !subtitle.isBlank()) {
            text.add(Box.createVerticalStrut(2));
            subtitleLabel.setText(subtitle);
            subtitleLabel.setFont(Theme.FONT_TINY);
            subtitleLabel.setForeground(Theme.c().textSecondary);
            subtitleLabel.setAlignmentX(LEFT_ALIGNMENT);
            text.add(subtitleLabel);
        }

        add(text, BorderLayout.WEST);

        trailing.setOpaque(false);
        add(trailing, BorderLayout.EAST);
    }

    public SectionHeader addTrailing(Component component) {
        trailing.add(component);
        return this;
    }

    public void setTitle(String title) {
        titleLabel.setText(title);
    }

    public void setSubtitle(String subtitle) {
        subtitleLabel.setText(subtitle);
    }
}
