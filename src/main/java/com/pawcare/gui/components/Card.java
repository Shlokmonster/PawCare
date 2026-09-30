package com.pawcare.gui.components;

import com.pawcare.theme.Theme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.LayoutManager;

/**
 * A rounded surface with an optional title header and a content area.
 *
 * <p>Almost every screen is built from these, which is what keeps padding, corner radius
 * and title styling identical everywhere.</p>
 */
public class Card extends RoundedPanel {

    private final JPanel content = new JPanel(new BorderLayout());
    private JPanel header;

    public Card() {
        this(null, null, null);
    }

    public Card(String title) {
        this(title, null, null);
    }

    public Card(String title, String subtitle) {
        this(title, subtitle, null);
    }

    /**
     * @param title    card heading, or null for a card without a header
     * @param subtitle small line under the heading
     * @param trailing component placed at the right of the header, such as a button
     */
    public Card(String title, String subtitle, java.awt.Component trailing) {
        super(new BorderLayout());
        setCardBackground(Theme.c().surface);
        setBorderColor(Theme.c().border);
        setRadius(Theme.RADIUS_CARD);
        setBorder(BorderFactory.createEmptyBorder(
                Theme.SPACE_LG, Theme.SPACE_LG, Theme.SPACE_LG, Theme.SPACE_LG));

        if (title != null) {
            header = buildHeader(title, subtitle, trailing);
            add(header, BorderLayout.NORTH);
        }

        content.setOpaque(false);
        if (header != null) {
            content.setBorder(BorderFactory.createEmptyBorder(Theme.SPACE_MD, 0, 0, 0));
        }
        add(content, BorderLayout.CENTER);
    }

    private JPanel buildHeader(String title, String subtitle, java.awt.Component trailing) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(Theme.FONT_H2);
        titleLabel.setForeground(Theme.c().textPrimary);
        titleLabel.setAlignmentX(LEFT_ALIGNMENT);
        text.add(titleLabel);

        if (subtitle != null && !subtitle.isBlank()) {
            text.add(Box.createVerticalStrut(2));
            JLabel subtitleLabel = new JLabel(subtitle);
            subtitleLabel.setFont(Theme.FONT_SMALL);
            subtitleLabel.setForeground(Theme.c().textSecondary);
            subtitleLabel.setAlignmentX(LEFT_ALIGNMENT);
            text.add(subtitleLabel);
        }

        panel.add(text, BorderLayout.WEST);
        if (trailing != null) {
            JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
            right.setOpaque(false);
            right.add(trailing);
            panel.add(right, BorderLayout.EAST);
        }
        return panel;
    }

    /** The area below the header where the card's content should be added. */
    public JPanel content() {
        return content;
    }

    /** Convenience: sets the body to a single component that fills the card. */
    public Card withContent(java.awt.Component component) {
        content.removeAll();
        content.add(component, BorderLayout.CENTER);
        return this;
    }

    /** Overrides the layout used by the body, e.g. for a form. */
    public Card withContentLayout(LayoutManager layout) {
        content.setLayout(layout);
        return this;
    }

    /** Adds a component to the body without removing what is already there. */
    public Card addContent(java.awt.Component component, Object constraints) {
        content.add(component, constraints);
        return this;
    }

    /** A fixed-height card, useful for the dashboard strips. */
    public Card withPreferredHeight(int height) {
        setPreferredSize(new Dimension(getPreferredSize().width, height));
        return this;
    }
}
