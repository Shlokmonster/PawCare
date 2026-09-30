package com.pawcare.gui.components;

import com.pawcare.theme.Theme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;

/**
 * The heading strip at the top of every page.
 *
 * <p>It carries the page title and description on the left, the clinic name and today's
 * date together with any page actions on the right, and a hairline rule underneath that
 * separates the heading from the content.</p>
 */
public class PageHeader extends JPanel {

    private final JLabel titleLabel = new JLabel();
    private final JLabel subtitleLabel = new JLabel();
    private final JLabel metaLabel = new JLabel();
    private final JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, Theme.SPACE_SM, 0));
    private final JPanel actionsRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));

    public PageHeader(String title, String subtitle) {
        setOpaque(false);
        setLayout(new BorderLayout(Theme.SPACE_LG, 0));
        setBorder(BorderFactory.createEmptyBorder(0, 0, Theme.SPACE_LG, 0));

        // Left: title and description.
        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));

        titleLabel.setText(title);
        titleLabel.setFont(Theme.FONT_H1);
        titleLabel.setForeground(Theme.c().textPrimary);
        titleLabel.setAlignmentX(LEFT_ALIGNMENT);
        text.add(titleLabel);

        text.add(Box.createVerticalStrut(4));

        subtitleLabel.setText(subtitle);
        subtitleLabel.setFont(Theme.FONT_SMALL);
        subtitleLabel.setForeground(Theme.c().textSecondary);
        subtitleLabel.setAlignmentX(LEFT_ALIGNMENT);
        text.add(subtitleLabel);

        add(text, BorderLayout.WEST);

        // Right: clinic line, then the action buttons.
        JPanel right = new JPanel();
        right.setOpaque(false);
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));

        metaLabel.setFont(Theme.FONT_TINY);
        metaLabel.setForeground(Theme.c().textMuted);
        metaLabel.setAlignmentX(RIGHT_ALIGNMENT);
        right.add(metaLabel);
        right.add(Box.createVerticalStrut(Theme.SPACE_SM));

        actions.setOpaque(false);
        actionsRow.setOpaque(false);
        actionsRow.add(actions);
        actionsRow.setAlignmentX(RIGHT_ALIGNMENT);
        right.add(actionsRow);

        add(right, BorderLayout.EAST);
    }

    /** Small line above the buttons, e.g. "PawCare Veterinary Clinic · Wednesday, 30 September 2026". */
    public void setMeta(String text) {
        metaLabel.setText(text);
    }

    public void setTitle(String title) {
        titleLabel.setText(title);
    }

    public void setSubtitle(String subtitle) {
        subtitleLabel.setText(subtitle);
    }

    /** Adds a button to the right-hand side of the header. */
    public PageHeader addAction(Component component) {
        actions.add(component);
        return this;
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(Theme.c().divider);
        g2.fillRect(0, getHeight() - 1, getWidth(), 1);
        g2.dispose();
    }
}
