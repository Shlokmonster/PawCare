package com.pawcare.gui.dialogs;

import com.pawcare.gui.components.Badges;
import com.pawcare.gui.components.Card;
import com.pawcare.gui.components.ModernButton;
import com.pawcare.gui.components.ScrollPanes;
import com.pawcare.model.Animal;
import com.pawcare.model.Appointment;
import com.pawcare.model.MedicalInfo;
import com.pawcare.model.Owner;
import com.pawcare.model.Pet;
import com.pawcare.model.Treatment;
import com.pawcare.model.Vaccination;
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
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.util.List;

/**
 * The read-only pet profile.
 *
 * <p>It gathers everything the clinic knows about one patient in a single scrollable
 * view: identity, ownership, the medical background, the species-specific clinical plan,
 * the vaccination card and the treatment history.</p>
 *
 * <p>The clinical plan is obtained by calling {@link Animal#treatmentPlan()} on an
 * {@code Animal} reference. This dialog never asks which species the patient is – the
 * correct implementation is chosen at run time by the JVM, which is the polymorphism
 * requirement demonstrated in the interface rather than only in the model.</p>
 */
public class PetDetailsDialog extends JDialog {

    private final ClinicService clinic;
    private final Pet pet;

    public PetDetailsDialog(Window owner, ClinicService clinic, Pet pet) {
        super(owner, ModalityType.APPLICATION_MODAL);
        this.clinic = clinic;
        this.pet = pet;

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

        body.add(buildIdentityCard());
        body.add(Box.createVerticalStrut(Theme.SPACE_MD));
        body.add(buildFactsCard());
        body.add(Box.createVerticalStrut(Theme.SPACE_MD));
        body.add(buildTreatmentPlanCard());
        body.add(Box.createVerticalStrut(Theme.SPACE_MD));
        body.add(buildMedicalCard());
        body.add(Box.createVerticalStrut(Theme.SPACE_MD));
        body.add(buildVaccinationCard());
        body.add(Box.createVerticalStrut(Theme.SPACE_MD));
        body.add(buildHistoryCard());
        body.add(Box.createVerticalStrut(Theme.SPACE_MD));
        body.add(buildAppointmentCard());

        JScrollPane scroll = ScrollPanes.transparent(body);
        scroll.setPreferredSize(new Dimension(660, 560));

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
        setLocationRelativeTo(owner);
    }

    // ------------------------------------------------------------------
    // Sections
    // ------------------------------------------------------------------

    private JComponent buildIdentityCard() {
        Card card = new Card();
        card.setShadow(false);

        JPanel row = new JPanel(new BorderLayout(Theme.SPACE_MD, 0));
        row.setOpaque(false);

        JLabel title = new JLabel(pet.getName());
        title.setFont(Theme.FONT_DISPLAY);
        title.setForeground(Theme.c().textPrimary);

        JPanel titleBlock = new JPanel();
        titleBlock.setOpaque(false);
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        title.setAlignmentX(LEFT_ALIGNMENT);
        titleBlock.add(title);
        titleBlock.add(Box.createVerticalStrut(Theme.SPACE_XS));

        JLabel subtitle = new JLabel(pet.getAnimalId() + " · " + pet.getSpecies().getLabel()
                + " · " + pet.getBreed());
        subtitle.setFont(Theme.FONT_SMALL);
        subtitle.setForeground(Theme.c().textSecondary);
        subtitle.setAlignmentX(LEFT_ALIGNMENT);
        titleBlock.add(subtitle);

        row.add(titleBlock, BorderLayout.CENTER);

        JPanel badges = new JPanel(new FlowLayout(FlowLayout.RIGHT, Theme.SPACE_SM, 0));
        badges.setOpaque(false);
        badges.add(Badges.badge(clinic.vaccinations().statusForPet(pet.getAnimalId())));
        row.add(badges, BorderLayout.EAST);

        card.content().add(row, BorderLayout.CENTER);
        return card;
    }

    private JComponent buildFactsCard() {
        Card card = new Card("Patient record");
        card.setShadow(false);

        JPanel grid = new JPanel(new GridLayout(0, 2, Theme.SPACE_LG, Theme.SPACE_SM));
        grid.setOpaque(false);

        Owner owner = clinic.owners().findById(pet.getOwnerId()).orElse(null);

        grid.add(fact("Owner", owner == null ? "Unassigned" : owner.getName()));
        grid.add(fact("Owner contact", owner == null ? "—" : owner.getPhone()));
        grid.add(fact("Gender", pet.getGender() == null ? "—" : pet.getGender().getLabel()));
        grid.add(fact("Age", pet.getAge() + (pet.getAge() == 1 ? " year" : " years")));
        grid.add(fact("Weight", String.format("%.2f kg", pet.getWeight())));
        grid.add(fact("Species detail", pet.speciesDetail()));
        grid.add(fact("Registered on", DateUtil.format(pet.getRegistrationDate())));
        grid.add(fact("Visits recorded",
                String.valueOf(clinic.appointments().byPet(pet.getAnimalId()).size())));
        grid.add(fact("Treatments recorded",
                String.valueOf(clinic.treatments().countForPet(pet.getAnimalId()))));
        grid.add(fact("Vaccinations recorded",
                String.valueOf(clinic.vaccinations().forPet(pet.getAnimalId()).size())));

        card.content().add(grid, BorderLayout.CENTER);
        return card;
    }

    /**
     * Renders the species-specific clinical plan.
     *
     * <p>{@code pet} is declared as {@link Animal} on purpose: the call below dispatches to
     * {@code Dog.treatmentPlan()}, {@code Cat.treatmentPlan()} or
     * {@code Bird.treatmentPlan()} depending on the actual object, with no type test in
     * sight.</p>
     */
    private JComponent buildTreatmentPlanCard() {
        Card card = new Card("Clinical treatment plan",
                "Produced by " + pet.getSpecies().getLabel() + ".treatmentPlan()");

        Animal animal = pet;   // upcast: the variable's type is the parent class

        JTextArea plan = new JTextArea(animal.treatmentPlan());
        plan.setEditable(false);
        plan.setFocusable(false);
        plan.setOpaque(false);
        plan.setLineWrap(true);
        plan.setWrapStyleWord(true);
        plan.setFont(Theme.FONT_BODY);
        plan.setForeground(Theme.c().textPrimary);
        plan.setBorder(BorderFactory.createEmptyBorder());
        plan.setSize(new Dimension(420, Short.MAX_VALUE));
        plan.setPreferredSize(new Dimension(420, plan.getPreferredSize().height));

        card.content().add(plan, BorderLayout.CENTER);
        return card;
    }

    private JComponent buildMedicalCard() {
        Card card = new Card("Medical background");
        card.setShadow(false);

        MedicalInfo medical = pet.getMedicalInfo();
        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));

        column.add(fact("Allergies", medical == null ? "None recorded" : medical.getAllergies()));
        column.add(Box.createVerticalStrut(Theme.SPACE_SM));
        column.add(fact("Chronic conditions",
                medical == null ? "None recorded" : medical.getChronicConditions()));
        column.add(Box.createVerticalStrut(Theme.SPACE_SM));
        column.add(fact("Notes", medical == null || medical.getNotes().isBlank()
                ? "None recorded" : medical.getNotes()));

        card.content().add(column, BorderLayout.CENTER);
        return card;
    }

    private JComponent buildVaccinationCard() {
        List<Vaccination> records = clinic.vaccinations().forPet(pet.getAnimalId());
        Card card = new Card("Vaccination card", records.size() + " record(s)");

        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));

        if (records.isEmpty()) {
            column.add(muted("No vaccination records for this patient yet."));
        } else {
            for (Vaccination vaccination : records) {
                JPanel row = new JPanel(new BorderLayout(Theme.SPACE_SM, 0));
                row.setOpaque(false);
                row.setBorder(BorderFactory.createEmptyBorder(
                        Theme.SPACE_XS, 0, Theme.SPACE_XS, 0));

                JLabel name = new JLabel(vaccination.getVaccineName() + "  ·  next due "
                        + DateUtil.format(vaccination.getNextDueDate()));
                name.setFont(Theme.FONT_SMALL);
                name.setForeground(Theme.c().textPrimary);
                row.add(name, BorderLayout.CENTER);

                JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, Theme.SPACE_SM, 0));
                right.setOpaque(false);
                right.add(Badges.badge(clinic.vaccinations().statusOf(vaccination)));
                row.add(right, BorderLayout.EAST);

                column.add(row);
            }
        }

        card.content().add(column, BorderLayout.CENTER);
        return card;
    }

    private JComponent buildHistoryCard() {
        List<Treatment> history = clinic.treatments().getTreatmentHistory(pet.getAnimalId());
        Card card = new Card("Treatment history",
                history.size() + " entr" + (history.size() == 1 ? "y" : "ies")
                        + " · oldest first (LinkedList order)");

        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));

        if (history.isEmpty()) {
            column.add(muted("No treatments recorded for this patient yet."));
        } else {
            for (Treatment treatment : history) {
                JPanel entry = new JPanel();
                entry.setOpaque(false);
                entry.setLayout(new BoxLayout(entry, BoxLayout.Y_AXIS));
                entry.setBorder(BorderFactory.createEmptyBorder(
                        Theme.SPACE_XS, 0, Theme.SPACE_SM, 0));
                entry.setAlignmentX(LEFT_ALIGNMENT);

                JLabel headline = new JLabel(DateUtil.format(treatment.getDate())
                        + "  ·  " + treatment.getDiagnosis());
                headline.setFont(Theme.FONT_SMALL_MEDIUM);
                headline.setForeground(Theme.c().textPrimary);
                headline.setAlignmentX(LEFT_ALIGNMENT);
                entry.add(headline);

                entry.add(Box.createVerticalStrut(2));

                JLabel detail = new JLabel(treatment.getTreatment()
                        + "  ·  " + safe(treatment.getMedication())
                        + "  ·  " + clinic.veterinarians().vetName(treatment.getVeterinarianId()));
                detail.setFont(Theme.FONT_TINY);
                detail.setForeground(Theme.c().textSecondary);
                detail.setAlignmentX(LEFT_ALIGNMENT);
                entry.add(detail);

                column.add(entry);
            }
        }

        card.content().add(column, BorderLayout.CENTER);
        return card;
    }

    private JComponent buildAppointmentCard() {
        List<Appointment> appointments = clinic.appointments().byPet(pet.getAnimalId());
        Card card = new Card("Appointments", appointments.size() + " booking(s)");

        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));

        if (appointments.isEmpty()) {
            column.add(muted("No appointments booked for this patient yet."));
        } else {
            for (Appointment appointment : appointments) {
                JPanel row = new JPanel(new BorderLayout(Theme.SPACE_SM, 0));
                row.setOpaque(false);
                row.setBorder(BorderFactory.createEmptyBorder(
                        Theme.SPACE_XS, 0, Theme.SPACE_XS, 0));

                JLabel line = new JLabel(DateUtil.format(appointment.getAppointmentDate(),
                        appointment.getAppointmentTime())
                        + "  ·  " + appointment.getReason()
                        + "  ·  " + clinic.veterinarians().vetName(appointment.getVeterinarianId()));
                line.setFont(Theme.FONT_SMALL);
                line.setForeground(Theme.c().textPrimary);
                row.add(line, BorderLayout.CENTER);

                JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, Theme.SPACE_SM, 0));
                right.setOpaque(false);
                right.add(Badges.badge(appointment.getStatus()));
                row.add(right, BorderLayout.EAST);

                column.add(row);
            }
        }

        card.content().add(column, BorderLayout.CENTER);
        return card;
    }

    // ------------------------------------------------------------------
    // Small builders
    // ------------------------------------------------------------------

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

    private static JLabel muted(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.FONT_SMALL);
        label.setForeground(Theme.c().textMuted);
        return label;
    }

    /** Renders an optional text field without ever printing the word "null". */
    private static String safe(String value) {
        return value == null || value.isBlank() ? "No medication" : value;
    }
}
