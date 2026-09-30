package com.pawcare.gui.dialogs;

import com.pawcare.gui.components.Card;
import com.pawcare.gui.components.ModernButton;
import com.pawcare.gui.components.ScrollPanes;
import com.pawcare.service.ClinicService;
import com.pawcare.theme.IconFactory;
import com.pawcare.theme.Theme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;

/**
 * The About screen: what the application is, what it is built with, and where it keeps
 * its data.
 *
 * <p>It is reached from the bottom of the navigation rail. The technology list is not
 * decoration – it is the same set of Java features the project is assessed on, listed in
 * one place for the viva.</p>
 */
public class AboutDialog extends JDialog {

    /** Shown on the About screen and in the window title. */
    public static final String APP_NAME = "PAWCARE";
    public static final String APP_SUBTITLE = "Complete Pet Health & Clinic Management";
    public static final String APP_VERSION = "Version 1.0";

    public AboutDialog(Window parent, ClinicService clinic) {
        super(parent, ModalityType.APPLICATION_MODAL);

        setUndecorated(true);
        try {
            setBackground(new Color(0, 0, 0, 0));
        } catch (UnsupportedOperationException ignored) {
            // Square corners on platforms without per-pixel translucency.
        }

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, Theme.SPACE_MD));

        body.add(buildBanner());
        body.add(Box.createVerticalStrut(Theme.SPACE_MD));
        body.add(buildPurposeCard());
        body.add(Box.createVerticalStrut(Theme.SPACE_MD));
        body.add(buildTechnologyCard());
        body.add(Box.createVerticalStrut(Theme.SPACE_MD));
        body.add(buildStorageCard(clinic));
        body.add(Box.createVerticalStrut(Theme.SPACE_MD));
        body.add(buildScopeCard());

        JScrollPane scroll = ScrollPanes.transparent(body);
        scroll.setPreferredSize(new Dimension(600, 480));

        ModernButton close = new ModernButton("Close", ModernButton.Variant.SECONDARY);
        close.addActionListener(e -> dispose());
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createEmptyBorder(Theme.SPACE_MD, 0, 0, Theme.SPACE_MD));
        footer.add(close);

        JPanel root = new JPanel(new BorderLayout());
        root.setOpaque(false);
        root.add(scroll, BorderLayout.CENTER);
        root.add(footer, BorderLayout.SOUTH);

        setContentPane(root);
        pack();
        setLocationRelativeTo(parent);
    }

    private JComponent buildBanner() {
        Card card = new Card();
        card.setShadow(false);

        JLabel mark = new JLabel(IconFactory.of("paw", 44, Theme.c().primary));

        JLabel name = new JLabel(APP_NAME);
        name.setFont(Theme.FONT_DISPLAY);
        name.setForeground(Theme.c().textPrimary);

        JLabel subtitle = new JLabel(APP_SUBTITLE);
        subtitle.setFont(Theme.FONT_BODY);
        subtitle.setForeground(Theme.c().textSecondary);

        JLabel version = new JLabel(APP_VERSION + " · Java desktop application");
        version.setFont(Theme.FONT_TINY);
        version.setForeground(Theme.c().textMuted);

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        name.setAlignmentX(LEFT_ALIGNMENT);
        subtitle.setAlignmentX(LEFT_ALIGNMENT);
        version.setAlignmentX(LEFT_ALIGNMENT);
        text.add(name);
        text.add(Box.createVerticalStrut(Theme.SPACE_XS));
        text.add(subtitle);
        text.add(Box.createVerticalStrut(Theme.SPACE_XS));
        text.add(version);

        JPanel row = new JPanel(new BorderLayout(Theme.SPACE_LG, 0));
        row.setOpaque(false);
        row.add(mark, BorderLayout.WEST);
        row.add(text, BorderLayout.CENTER);

        card.content().add(row, BorderLayout.CENTER);
        return card;
    }

    private JComponent buildPurposeCard() {
        Card card = new Card("What this application does");
        card.setShadow(false);
        card.content().add(paragraph(
                "PawCare is the front-desk system for a small veterinary clinic. It keeps the "
                        + "register of patients and their owners, the appointment diary, the clinical "
                        + "history of every animal and the vaccination card, and it turns that data "
                        + "into the reminders and reports the clinic runs on. All information is "
                        + "stored locally on the machine, so the application works with no network "
                        + "connection and no database server."),
                BorderLayout.CENTER);
        return card;
    }

    private JComponent buildTechnologyCard() {
        Card card = new Card("Built with");
        card.setShadow(false);

        JPanel grid = new JPanel(new java.awt.GridLayout(0, 2, Theme.SPACE_LG, Theme.SPACE_SM));
        grid.setOpaque(false);

        grid.add(bullet("Java 17", "Language and standard library"));
        grid.add(bullet("Java Swing", "Desktop user interface"));
        grid.add(bullet("Collections Framework", "ArrayList, LinkedList, HashMap, TreeMap"));
        grid.add(bullet("java.time", "LocalDate, LocalTime, LocalDateTime"));
        grid.add(bullet("Java Serialization", "Data files under data/"));
        grid.add(bullet("JUnit 5", "Automated unit tests"));
        grid.add(bullet("Maven", "Build and dependency management"));
        grid.add(bullet("Java2D", "Icons, charts and custom painting"));

        card.content().add(grid, BorderLayout.CENTER);
        return card;
    }

    private JComponent buildStorageCard(ClinicService clinic) {
        Card card = new Card("Where the data lives");
        card.setShadow(false);

        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));

        column.add(paragraph("Clinic records are saved as serialized Java objects in:"));
        column.add(Box.createVerticalStrut(Theme.SPACE_SM));

        JLabel path = new JLabel(clinic.store().getDataDirectory().toAbsolutePath().toString());
        path.setFont(Theme.FONT_SMALL_MEDIUM);
        path.setForeground(Theme.c().primary);
        path.setAlignmentX(LEFT_ALIGNMENT);
        column.add(path);

        column.add(Box.createVerticalStrut(Theme.SPACE_SM));
        column.add(paragraph("Preferences such as the theme and the reminder window are kept in "
                + "pawcare-settings.properties beside it. A damaged data file is moved aside with "
                + "a .corrupt suffix instead of stopping the application."));

        card.content().add(column, BorderLayout.CENTER);
        return card;
    }

    private JComponent buildScopeCard() {
        Card card = new Card("Project scope");
        card.setShadow(false);

        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));

        column.add(paragraph("B.Tech Computer Science & Engineering — Semester III "
                + "Java Programming project."));
        column.add(Box.createVerticalStrut(Theme.SPACE_SM));
        column.add(paragraph("The design keeps every layer separate — model, service, "
                + "repository and interface — so each part can be explained and tested on "
                + "its own."));

        card.content().add(column, BorderLayout.CENTER);
        return card;
    }

    private static JComponent paragraph(String text) {
        JLabel label = new JLabel("<html><body style='width:430px'>" + text + "</body></html>");
        label.setFont(Theme.FONT_SMALL);
        label.setForeground(Theme.c().textSecondary);
        label.setAlignmentX(LEFT_ALIGNMENT);
        return label;
    }

    private static JComponent bullet(String title, String detail) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel heading = new JLabel(title);
        heading.setFont(Theme.FONT_SMALL_MEDIUM);
        heading.setForeground(Theme.c().textPrimary);
        heading.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(heading);

        JLabel caption = new JLabel(detail);
        caption.setFont(Theme.FONT_TINY);
        caption.setForeground(Theme.c().textSecondary);
        caption.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(caption);

        return panel;
    }
}
