package com.pawcare.gui.components;

import com.pawcare.theme.Theme;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;

/**
 * Builds the label-and-field layout used by every dialog.
 *
 * <p>A {@link GridBagLayout} gives the labels a fixed column and lets the fields take the
 * remaining width, so forms in different dialogs line up with each other without any
 * manual tweaking.</p>
 */
public class FormPanel extends JPanel {

    /** Fixed width of the label column, so every dialog aligns identically. */
    private static final int LABEL_WIDTH = 130;

    private int row = 0;

    public FormPanel() {
        setOpaque(false);
        setLayout(new GridBagLayout());
    }

    /** A small uppercase heading that starts a group of fields. */
    public FormPanel section(String title) {
        JLabel label = new JLabel(title.toUpperCase());
        label.setFont(Theme.FONT_TINY);
        label.setForeground(Theme.c().textMuted);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = row++;
        constraints.gridwidth = 2;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1;
        constraints.insets = new Insets(row == 1 ? 0 : Theme.SPACE_LG, 0, Theme.SPACE_SM, 0);
        add(label, constraints);
        return this;
    }

    /** One label and one field on a new line. */
    public FormPanel row(String label, JComponent field) {
        addLabel(label, row, false);
        addField(field, row, 1, false);
        row++;
        return this;
    }

    /**
     * One <i>custom</i> label and one field on a new line.
     *
     * <p>Used when the label itself has to change later – the pet form swaps between
     * "Training level", "Lifestyle" and "Wing span" as the species changes, and needs to
     * keep the reference in order to retitle it.</p>
     */
    public FormPanel row(JLabel label, JComponent field) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = row;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(0, 0, Theme.SPACE_MD, Theme.SPACE_MD);
        add(label, constraints);

        addField(field, row, 1, false);
        row++;
        return this;
    }

    /**
     * Creates a label styled exactly like the ones this panel builds itself, so a custom
     * row is indistinguishable from a generated one.
     */
    public static JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.FONT_SMALL_MEDIUM);
        label.setForeground(Theme.c().textSecondary);
        label.setPreferredSize(new Dimension(LABEL_WIDTH, 20));
        return label;
    }

    /** One label and two fields side by side, e.g. date and time. */
    public FormPanel pair(String label, JComponent first, JComponent second) {
        JPanel holder = new JPanel(new GridLayout(1, 2, Theme.SPACE_SM, 0));
        holder.setOpaque(false);
        holder.add(first);
        holder.add(second);

        addLabel(label, row, false);
        addField(holder, row, 1, false);
        row++;
        return this;
    }

    /** A field that spans the full width, with no label. */
    public FormPanel fullWidth(JComponent field) {
        addField(field, row, 2, false);
        row++;
        return this;
    }

    /** A field with a label above it rather than beside it, used for notes boxes. */
    public FormPanel stacked(String label, JComponent field) {
        JPanel holder = new JPanel();
        holder.setOpaque(false);
        holder.setLayout(new BoxLayout(holder, BoxLayout.Y_AXIS));

        JLabel caption = new JLabel(label);
        caption.setFont(Theme.FONT_SMALL_MEDIUM);
        caption.setForeground(Theme.c().textSecondary);
        caption.setAlignmentX(LEFT_ALIGNMENT);
        holder.add(caption);
        holder.add(Box.createVerticalStrut(Theme.SPACE_XS));

        field.setAlignmentX(LEFT_ALIGNMENT);
        holder.add(field);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = row++;
        constraints.gridwidth = 2;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(0, 0, Theme.SPACE_MD, 0);
        add(holder, constraints);
        return this;
    }

    /** A muted explanatory line, spanning both columns. */
    public FormPanel hint(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.FONT_TINY);
        label.setForeground(Theme.c().textMuted);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = row++;
        constraints.gridwidth = 2;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(0, 0, Theme.SPACE_MD, 0);
        add(label, constraints);
        return this;
    }

    /** Absorbs any leftover vertical space so the fields stay at the top. */
    public FormPanel finish() {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = row;
        constraints.gridwidth = 2;
        constraints.weighty = 1;
        constraints.fill = GridBagConstraints.VERTICAL;
        add(Box.createVerticalGlue(), constraints);
        return this;
    }

    private void addLabel(String text, int gridy, boolean topAligned) {
        JLabel label = createLabel(text);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = gridy;
        constraints.anchor = topAligned ? GridBagConstraints.NORTHWEST : GridBagConstraints.WEST;
        constraints.insets = new Insets(0, 0, Theme.SPACE_MD, Theme.SPACE_MD);
        add(label, constraints);
    }

    private void addField(JComponent field, int gridy, int width, boolean topAligned) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = gridy;
        constraints.gridwidth = width;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = topAligned ? GridBagConstraints.NORTHWEST : GridBagConstraints.WEST;
        constraints.insets = new Insets(0, 0, Theme.SPACE_MD, 0);
        add(field, constraints);
    }
}
