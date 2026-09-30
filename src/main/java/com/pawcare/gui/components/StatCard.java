package com.pawcare.gui.components;

import com.pawcare.theme.Colors;
import com.pawcare.theme.IconFactory;
import com.pawcare.theme.Theme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;

/**
 * A single figure on the dashboard, for example "Total Pets — 8".
 *
 * <p>Laid out as a label, a large value and a caption, with a tinted icon tile on the
 * right. Every number it shows is passed in by the caller after being computed from the
 * live data.</p>
 */
public class StatCard extends Card {

    private final JLabel valueLabel = new JLabel();
    private final JLabel captionLabel = new JLabel();

    public StatCard(String label, String value, String caption, String iconName, Color accent) {
        super();
        setShadow(false);
        setBorder(BorderFactory.createEmptyBorder(
                Theme.SPACE_LG, Theme.SPACE_LG, Theme.SPACE_LG, Theme.SPACE_LG));

        JPanel row = new JPanel(new BorderLayout(Theme.SPACE_MD, 0));
        row.setOpaque(false);

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));

        JLabel labelView = new JLabel(label.toUpperCase());
        labelView.setFont(Theme.FONT_TINY);
        labelView.setForeground(Theme.c().textSecondary);
        labelView.setAlignmentX(LEFT_ALIGNMENT);
        text.add(labelView);

        text.add(Box.createVerticalStrut(Theme.SPACE_SM));

        valueLabel.setText(value);
        valueLabel.setFont(Theme.FONT_DISPLAY);
        valueLabel.setForeground(Theme.c().textPrimary);
        valueLabel.setAlignmentX(LEFT_ALIGNMENT);
        text.add(valueLabel);

        text.add(Box.createVerticalStrut(Theme.SPACE_XS));

        captionLabel.setText(caption);
        captionLabel.setFont(Theme.FONT_TINY);
        captionLabel.setForeground(accent);
        captionLabel.setAlignmentX(LEFT_ALIGNMENT);
        text.add(captionLabel);

        row.add(text, BorderLayout.CENTER);
        row.add(buildIconTile(iconName, accent), BorderLayout.EAST);

        content().add(row, BorderLayout.CENTER);
        setPreferredSize(new Dimension(240, 116));
    }

    private JPanel buildIconTile(String iconName, Color accent) {
        RoundedPanel tile = new RoundedPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        tile.setRadius(12);
        tile.setCardBackground(Colors.alpha(accent, 34));
        tile.setPreferredSize(new Dimension(44, 44));

        JLabel icon = new JLabel(IconFactory.of(iconName, 21, accent));
        icon.setBorder(BorderFactory.createEmptyBorder(11, 0, 0, 0));
        tile.add(icon);

        JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        wrapper.setOpaque(false);
        wrapper.add(tile);
        return wrapper;
    }

    /** Updates the figure without rebuilding the card. */
    public void setValue(String value) {
        valueLabel.setText(value);
    }

    /** Updates the small line under the figure, e.g. "3 overdue". */
    public void setCaption(String caption) {
        captionLabel.setText(caption);
    }

    /** Recolours the caption, used when a state turns urgent. */
    public void setCaptionColor(Color color) {
        captionLabel.setForeground(color);
    }
}
