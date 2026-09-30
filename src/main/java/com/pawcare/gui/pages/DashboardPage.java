package com.pawcare.gui.pages;

import com.pawcare.gui.Page;
import com.pawcare.gui.components.BarChartPanel;
import com.pawcare.gui.components.Card;
import com.pawcare.gui.components.DonutChartPanel;
import com.pawcare.gui.components.ListCard;
import com.pawcare.gui.components.ModernButton;
import com.pawcare.gui.components.SectionHeader;
import com.pawcare.gui.components.StatCard;
import com.pawcare.gui.dialogs.AppointmentDialog;
import com.pawcare.model.Appointment;
import com.pawcare.model.Treatment;
import com.pawcare.model.Vaccination;
import com.pawcare.model.enums.AppointmentStatus;
import com.pawcare.model.enums.Species;
import com.pawcare.model.enums.VaccinationStatus;
import com.pawcare.service.ClinicService;
import com.pawcare.service.ReportService;
import com.pawcare.theme.Theme;
import com.pawcare.util.DateUtil;

import java.awt.BorderLayout;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The opening screen: the state of the clinic at a glance.
 *
 * <p>Every number, list and chart on this page is read from {@link ReportService}, which
 * computes it from the live collections. Nothing here is hard-coded, so adding a pet or
 * cancelling an appointment changes this screen the moment it is refreshed.</p>
 */
public class DashboardPage extends Page {

    /** How many reminder and history rows a dashboard card shows before it scrolls. */
    private static final int MAX_LIST_ROWS = 8;
    private static final int LIST_CARD_HEIGHT = 320;

    public DashboardPage(ClinicService clinic) {
        super(clinic, "Dashboard",
                "A live picture of the clinic. Every figure below is computed from the stored records.");

        ModernButton book = new ModernButton("Book appointment",
                ModernButton.Variant.PRIMARY, "plus");
        book.addActionListener(e -> openAppointmentForm());
        header().addAction(book);
    }

    @Override
    public void refresh() {
        clearBody();
        addOverview();
        addTrends();
        addAttention();
        rebuildFinished();
    }

    // ------------------------------------------------------------------
    // Overview strip
    // ------------------------------------------------------------------

    private void addOverview() {
        ReportService reports = clinic.reports();
        int overdue = clinic.vaccinations().countOverdue();
        int alerts = reports.vaccinationAlertCount();

        StatCard patients = new StatCard("Registered patients",
                String.valueOf(reports.totalPets()),
                speciesCaption(), "paw", Theme.c().primary);

        StatCard visits = new StatCard("Visits today",
                String.valueOf(reports.todaysVisitCount()),
                todayCaption(), "appointments", Theme.c().info);

        StatCard reminders = new StatCard("Vaccination alerts",
                String.valueOf(alerts),
                overdue > 0 ? overdue + " overdue" : "None overdue",
                "vaccinations", overdue > 0 ? Theme.c().danger : Theme.c().warning);

        StatCard owners = new StatCard("Registered owners",
                String.valueOf(reports.totalOwners()),
                reports.totalVeterinarians() + " veterinarians on the team",
                "owners", Theme.c().accent);

        stack(section("Overview", reports.clinicStatusLine()), Theme.SPACE_MD);
        stack(row(patients, visits, reminders, owners), Theme.SPACE_XL);
    }

    /** e.g. "3 dogs · 3 cats · 2 birds". */
    private String speciesCaption() {
        Map<Species, Integer> counts = clinic.reports().petsBySpecies();
        StringBuilder text = new StringBuilder();
        for (Species species : Species.values()) {
            int count = counts.getOrDefault(species, 0);
            if (text.length() > 0) {
                text.append("  ·  ");
            }
            text.append(count).append(' ').append(species.getLabel().toLowerCase());
            if (count != 1) {
                text.append('s');
            }
        }
        return text.toString();
    }

    /** How many of today's visits are still open and how many are already done. */
    private String todayCaption() {
        List<Appointment> today = clinic.reports().todaysAppointments();
        int scheduled = 0;
        int completed = 0;
        for (Appointment appointment : today) {
            if (appointment.getStatus() == AppointmentStatus.SCHEDULED) {
                scheduled++;
            } else if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
                completed++;
            }
        }
        return scheduled + " scheduled  ·  " + completed + " completed";
    }

    // ------------------------------------------------------------------
    // Charts
    // ------------------------------------------------------------------

    private void addTrends() {
        Card speciesCard = new Card("Patients by species", "Share of the current register");
        DonutChartPanel species = new DonutChartPanel();
        species.setCentreCaption("patients");
        species.setEmptyMessage("No patients registered yet");
        species.setSlices(speciesSlices());
        speciesCard.content().add(species, BorderLayout.CENTER);

        Card statusCard = new Card("Appointments by status", "Every booking on record");
        BarChartPanel status = new BarChartPanel();
        status.setEmptyMessage("No appointments recorded yet");
        status.setBars(statusBars());
        statusCard.content().add(status, BorderLayout.CENTER);

        stack(section("Trends", null), Theme.SPACE_MD);
        stack(row(speciesCard, statusCard), Theme.SPACE_XL);
    }

    private List<DonutChartPanel.Slice> speciesSlices() {
        Map<Species, Integer> counts = clinic.reports().petsBySpecies();
        List<DonutChartPanel.Slice> slices = new ArrayList<>();
        int index = 0;
        for (Species species : Species.values()) {
            slices.add(new DonutChartPanel.Slice(species.getLabel(),
                    counts.getOrDefault(species, 0), seriesColour(index++)));
        }
        return slices;
    }

    private List<BarChartPanel.Bar> statusBars() {
        Map<AppointmentStatus, Integer> counts = clinic.reports().appointmentsByStatus();
        List<BarChartPanel.Bar> bars = new ArrayList<>();
        bars.add(new BarChartPanel.Bar("Scheduled",
                counts.getOrDefault(AppointmentStatus.SCHEDULED, 0), Theme.c().info));
        bars.add(new BarChartPanel.Bar("Completed",
                counts.getOrDefault(AppointmentStatus.COMPLETED, 0), Theme.c().success));
        bars.add(new BarChartPanel.Bar("Cancelled",
                counts.getOrDefault(AppointmentStatus.CANCELLED, 0), Theme.c().danger));
        return bars;
    }

    /** Cycles through the palette's categorical colours so a chart never runs out. */
    private static java.awt.Color seriesColour(int index) {
        java.awt.Color[] palette = Theme.c().chart;
        return palette[index % palette.length];
    }

    // ------------------------------------------------------------------
    // Operational lists
    // ------------------------------------------------------------------

    private void addAttention() {
        stack(section("What needs attention", "Today's diary, reminders and the latest clinical entries"),
                Theme.SPACE_MD);
        stack(row(buildTodaysCard(), buildAlertsCard(), buildRecentCard()), 0);
    }

    private ListCard buildTodaysCard() {
        ListCard card = new ListCard("Today's appointments",
                DateUtil.formatLong(LocalDate.now()),
                "Nothing booked today",
                "The diary is clear. Book a visit from the Appointments screen.");

        for (Appointment appointment : clinic.reports().todaysAppointments()) {
            card.addRow("clock", Theme.c().info,
                    clinic.pets().petName(appointment.getPetId()),
                    appointment.getReason() + "  ·  "
                            + clinic.veterinarians().vetName(appointment.getVeterinarianId()),
                    DateUtil.format(appointment.getAppointmentTime()),
                    appointment.getStatus());
        }
        card.refresh();
        return fixed(card, LIST_CARD_HEIGHT);
    }

    private ListCard buildAlertsCard() {
        ListCard card = new ListCard("Vaccination reminders",
                "Overdue and due within " + clinic.vaccinations().getReminderDays() + " days",
                "Nothing pending",
                "Every patient's vaccination card is up to date.");

        int shown = 0;
        for (Vaccination vaccination : clinic.reports().vaccinationAlerts()) {
            if (shown++ >= MAX_LIST_ROWS) {
                break;
            }
            VaccinationStatus status = clinic.vaccinations().statusOf(vaccination);
            card.addRow("bell", Theme.badgeText(status),
                    clinic.pets().petName(vaccination.getPetId())
                            + "  ·  " + vaccination.getVaccineName(),
                    DateUtil.overdueLabel(vaccination.getNextDueDate()),
                    DateUtil.format(vaccination.getNextDueDate()),
                    status);
        }
        card.refresh();
        return fixed(card, LIST_CARD_HEIGHT);
    }

    private ListCard buildRecentCard() {
        ListCard card = new ListCard("Recent treatments", "The latest clinical entries",
                "No treatments recorded",
                "Treatments appear here as soon as they are logged.");

        for (Treatment treatment : clinic.reports().recentTreatments(MAX_LIST_ROWS)) {
            card.addRow("treatments", Theme.c().accent,
                    clinic.pets().petName(treatment.getPetId())
                            + "  ·  " + treatment.getDiagnosis(),
                    treatment.getTreatment(),
                    DateUtil.format(treatment.getDate()),
                    null);
        }
        card.refresh();
        return fixed(card, LIST_CARD_HEIGHT);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private SectionHeader section(String title, String subtitle) {
        return subtitle == null ? new SectionHeader(title) : new SectionHeader(title, subtitle);
    }

    private void openAppointmentForm() {
        if (clinic.pets().count() == 0 || clinic.veterinarians().count() == 0) {
            com.pawcare.gui.dialogs.Dialogs.warning(this, "Cannot book a visit",
                    "Register at least one patient and have a veterinarian on the team "
                            + "before booking an appointment.");
            return;
        }
        AppointmentDialog dialog = new AppointmentDialog(window(), clinic, null);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            refresh();
        }
    }
}
