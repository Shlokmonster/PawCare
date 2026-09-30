package com.pawcare.gui.dialogs;

import com.pawcare.gui.components.Badges;
import com.pawcare.gui.components.Card;
import com.pawcare.gui.components.ModernButton;
import com.pawcare.gui.components.ScrollPanes;
import com.pawcare.model.Owner;
import com.pawcare.model.Pet;
import com.pawcare.service.ClinicService;
import com.pawcare.theme.Theme;
import com.pawcare.util.DateUtil;

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
import java.awt.GridLayout;
import java.awt.Window;
import java.util.List;

/**
 * The read-only client profile: contact details plus every pet registered to them.
 *
 * <p>Showing the pets alongside the owner makes the relationship in the data model
 * concrete – a {@code Pet} stores its owner's id, and this screen performs exactly the
 * reverse lookup the owner index is built for.</p>
 */
public class OwnerDetailsDialog extends JDialog {

    private final ClinicService clinic;
    private final Owner owner;

    public OwnerDetailsDialog(Window parent, ClinicService clinic, Owner owner) {
        super(parent, ModalityType.APPLICATION_MODAL);
        this.clinic = clinic;
        this.owner = owner;

        setUndecorated(true);
        try {
            setBackground(new Color(0, 0, 0, 0));
        } catch (UnsupportedOperationException ignored) {
            // Square corners on platforms without per-pixel translucency.
        }

        List<Pet> pets = clinic.pets().findByOwner(owner.getOwnerId());

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, Theme.SPACE_MD));

        body.add(buildIdentityCard(pets.size()));
        body.add(Box.createVerticalStrut(Theme.SPACE_MD));
        body.add(buildContactCard());
        body.add(Box.createVerticalStrut(Theme.SPACE_MD));
        body.add(buildPetsCard(pets));

        JScrollPane scroll = ScrollPanes.transparent(body);
        scroll.setPreferredSize(new Dimension(600, 430));

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

    private JComponent buildIdentityCard(int petCount) {
        Card card = new Card();
        card.setShadow(false);

        JLabel name = new JLabel(owner.getName());
        name.setFont(Theme.FONT_DISPLAY);
        name.setForeground(Theme.c().textPrimary);
        name.setAlignmentX(LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel(owner.getOwnerId() + " · " + petCount
                + (petCount == 1 ? " registered pet" : " registered pets"));
        subtitle.setFont(Theme.FONT_SMALL);
        subtitle.setForeground(Theme.c().textSecondary);
        subtitle.setAlignmentX(LEFT_ALIGNMENT);

        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        column.add(name);
        column.add(Box.createVerticalStrut(Theme.SPACE_XS));
        column.add(subtitle);

        card.content().add(column, BorderLayout.CENTER);
        return card;
    }

    private JComponent buildContactCard() {
        Card card = new Card("Contact details");
        card.setShadow(false);

        JPanel grid = new JPanel(new GridLayout(0, 2, Theme.SPACE_LG, Theme.SPACE_SM));
        grid.setOpaque(false);

        grid.add(fact("Mobile number", owner.getPhone()));
        grid.add(fact("Email", owner.getEmail()));
        grid.add(fact("Emergency contact", owner.getEmergencyContact()));
        grid.add(fact("Client since", earliestRegistration()));

        card.content().add(grid, BorderLayout.NORTH);

        JLabel address = new JLabel(owner.getAddress());
        address.setFont(Theme.FONT_SMALL);
        address.setForeground(Theme.c().textSecondary);
        address.setBorder(BorderFactory.createEmptyBorder(Theme.SPACE_MD, 0, 0, 0));
        card.content().add(address, BorderLayout.CENTER);

        return card;
    }

    private JComponent buildPetsCard(List<Pet> pets) {
        Card card = new Card("Registered pets", pets.size() + " in total");

        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));

        if (pets.isEmpty()) {
            JLabel empty = new JLabel("No pets are registered to this owner yet.");
            empty.setFont(Theme.FONT_SMALL);
            empty.setForeground(Theme.c().textMuted);
            column.add(empty);
        } else {
            for (Pet pet : pets) {
                JPanel row = new JPanel(new BorderLayout(Theme.SPACE_SM, 0));
                row.setOpaque(false);
                row.setBorder(BorderFactory.createEmptyBorder(
                        Theme.SPACE_XS, 0, Theme.SPACE_XS, 0));

                JLabel line = new JLabel(pet.getName() + "  ·  " + pet.getAnimalId()
                        + "  ·  " + pet.getSpecies().getLabel() + "  ·  " + pet.getBreed()
                        + "  ·  " + DateUtil.format(pet.getRegistrationDate()));
                line.setFont(Theme.FONT_SMALL);
                line.setForeground(Theme.c().textPrimary);
                row.add(line, BorderLayout.CENTER);

                JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, Theme.SPACE_SM, 0));
                right.setOpaque(false);
                right.add(Badges.badge(clinic.vaccinations().statusForPet(pet.getAnimalId())));
                row.add(right, BorderLayout.EAST);

                column.add(row);
            }
        }

        card.content().add(column, BorderLayout.CENTER);
        return card;
    }

    /** The registration date of the owner's earliest pet, used as a "client since" date. */
    private String earliestRegistration() {
        return clinic.pets().findByOwner(owner.getOwnerId()).stream()
                .map(Pet::getRegistrationDate)
                .filter(java.util.Objects::nonNull)
                .min(java.time.LocalDate::compareTo)
                .map(DateUtil::format)
                .orElse("—");
    }

    private static JComponent fact(String label, String value) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel caption = new JLabel(label.toUpperCase());
        caption.setFont(Theme.FONT_TINY);
        caption.setForeground(Theme.c().textMuted);
        caption.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(caption);
        panel.add(Box.createVerticalStrut(2));

        JLabel content = new JLabel(value == null || value.isBlank() ? "—" : value);
        content.setFont(Theme.FONT_BODY_MEDIUM);
        content.setForeground(Theme.c().textPrimary);
        content.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(content);

        return panel;
    }
}
