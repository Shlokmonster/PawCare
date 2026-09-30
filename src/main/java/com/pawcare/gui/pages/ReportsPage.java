package com.pawcare.gui.pages;

import com.pawcare.gui.Page;
import com.pawcare.gui.components.BarChartPanel;
import com.pawcare.gui.components.Card;
import com.pawcare.gui.components.DonutChartPanel;
import com.pawcare.gui.components.ListCard;
import com.pawcare.gui.components.ModernButton;
import com.pawcare.gui.components.SectionHeader;
import com.pawcare.gui.components.StatCard;
import com.pawcare.model.Appointment;
import com.pawcare.model.Owner;
import com.pawcare.model.Vaccination;
import com.pawcare.model.enums.AppointmentStatus;
import com.pawcare.model.enums.Species;
import com.pawcare.model.enums.VaccinationStatus;
import com.pawcare.service.ClinicService;
import com.pawcare.service.ReportService;
import com.pawcare.theme.Theme;
import com.pawcare.util.DateUtil;

import java.awt.BorderLayout;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Management reporting over the whole clinic.
 *
 * <p>Nothing on this page is stored. Every figure is recalculated from the live
 * collections each time the page is opened, so a report can never disagree with the
 * screen it summarises.</p>
 *
 * <p>The three charts answer the three questions a clinic manager actually asks: how the
 * patient base is made up, how the diary is performing, and how much vaccination work is
 * outstanding.</p>
 */
public class ReportsPage extends Page {

    private static final int MAX_LIST_ROWS = 8;
    private static final int CHART_ROW_HEIGHT = 330;
    private static final int LIST_ROW_HEIGHT = 310;

    public ReportsPage(ClinicService clinic) {
        super(clinic, "Reports",
                "Clinic-wide totals, breakdowns and outstanding work, computed from the stored records.");

        ModernButton refresh = new ModernButton("Refresh report",
                ModernButton.Variant.SECONDARY, "refresh");
        refresh.addActionListener(e -> {
            refresh();
            toast("Report recalculated");
        });
        header().addAction(refresh);
    }

    @Override
    public void refresh() {
        clearBody();
        addTotals();
        addBreakdowns();
        addHighlights();
        rebuildFinished();
    }

    // ------------------------------------------------------------------
    // Totals
    // ------------------------------------------------------------------

    private void addTotals() {
        ReportService reports = clinic.reports();

        stack(section("Register", "What the clinic is looking after"),
                Theme.SPACE_MD);
        stack(row(
                new StatCard("Registered patients", String.valueOf(reports.totalPets()),
                        reports.totalAppointments() + " appointments on record",
                        "paw", Theme.c().primary),
                new StatCard("Registered owners", String.valueOf(reports.totalOwners()),
                        averagePetsPerOwner() + " patients per owner on average",
                        "owners", Theme.c().accent),
                new StatCard("Veterinarians", String.valueOf(reports.totalVeterinarians()),
                        "on the clinic team", "owners", Theme.c().info)),
                Theme.SPACE_XL);

        stack(section("Activity", "Everything recorded so far"), Theme.SPACE_MD);
        stack(row(
                new StatCard("Appointments", String.valueOf(reports.totalAppointments()),
                        reports.visitsThisWeek() + " in the last seven days",
                        "appointments", Theme.c().info),
                new StatCard("Treatments", String.valueOf(reports.totalTreatments()),
                        clinic.treatments().countRecent(30) + " in the last thirty days",
                        "treatments", Theme.c().accent),
                new StatCard("Vaccinations", String.valueOf(reports.totalVaccinations()),
                        reports.vaccinationAlertCount() + " needing attention",
                        "vaccinations", Theme.c().warning)),
                Theme.SPACE_XL);
    }

    private String averagePetsPerOwner() {
        int owners = clinic.owners().count();
        if (owners == 0) {
            return "0.0";
        }
        return String.format(Locale.ENGLISH, "%.1f", clinic.pets().count() / (double) owners);
    }

    // ------------------------------------------------------------------
    // Charts
    // ------------------------------------------------------------------

    private void addBreakdowns() {
        stack(section("Breakdowns", "Where the cases sit today"), Theme.SPACE_MD);
        stack(fixed(row(buildSpeciesCard(), buildAppointmentCard(), buildVaccinationCard()),
                CHART_ROW_HEIGHT), Theme.SPACE_XL);
    }

    private Card buildSpeciesCard() {
        Card card = new Card("Patients by species", "Share of the current register");
        DonutChartPanel chart = new DonutChartPanel();
        chart.setCentreCaption("patients");
        chart.setEmptyMessage("No patients registered yet");
        chart.setSlices(speciesSlices());
        card.content().add(chart, BorderLayout.CENTER);
        return card;
    }

    private Card buildAppointmentCard() {
        Card card = new Card("Appointments by status", "How the diary is performing");
        BarChartPanel chart = new BarChartPanel();
        chart.setEmptyMessage("No appointments recorded yet");
        chart.setBars(appointmentBars());
        card.content().add(chart, BorderLayout.CENTER);
        return card;
    }

    private Card buildVaccinationCard() {
        Card card = new Card("Vaccinations by state", "Outstanding vaccination work");
        BarChartPanel chart = new BarChartPanel();
        chart.setEmptyMessage("No vaccination records yet");
        chart.setBars(vaccinationBars());
        card.content().add(chart, BorderLayout.CENTER);
        return card;
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

    private List<BarChartPanel.Bar> appointmentBars() {
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

    private List<BarChartPanel.Bar> vaccinationBars() {
        Map<VaccinationStatus, Integer> counts = clinic.reports().vaccinationsByStatus();
        List<BarChartPanel.Bar> bars = new ArrayList<>();
        bars.add(new BarChartPanel.Bar("Overdue",
                counts.getOrDefault(VaccinationStatus.OVERDUE, 0), Theme.c().danger));
        bars.add(new BarChartPanel.Bar("Due Soon",
                counts.getOrDefault(VaccinationStatus.DUE_SOON, 0), Theme.c().warning));
        bars.add(new BarChartPanel.Bar("Upcoming",
                counts.getOrDefault(VaccinationStatus.UPCOMING, 0), Theme.c().success));
        return bars;
    }

    private static Color seriesColour(int index) {
        Color[] palette = Theme.c().chart;
        return palette[index % palette.length];
    }

    // ------------------------------------------------------------------
    // Highlights
    // ------------------------------------------------------------------

    private void addHighlights() {
        stack(section("Highlights", "The lists a manager looks at first"), Theme.SPACE_MD);
        stack(fixed(row(buildTopOwnersCard(), buildAlertsCard(), buildUpcomingCard()),
                LIST_ROW_HEIGHT), 0);
    }

    private ListCard buildTopOwnersCard() {
        ListCard card = new ListCard("Top clients by patients",
                "Owners ranked by how many pets they have registered",
                "No owners yet",
                "Owners appear here once they have pets registered.");

        for (Map.Entry<Owner, Integer> entry : clinic.reports().topOwnersByPetCount(MAX_LIST_ROWS)) {
            Owner owner = entry.getKey();
            int pets = entry.getValue();
            card.addRow("owners", Theme.c().primary,
                    owner.getName(),
                    owner.getPhone() + "  ·  " + owner.getEmail(),
                    pets + (pets == 1 ? " pet" : " pets"),
                    null);
        }
        card.refresh();
        return card;
    }

    private ListCard buildAlertsCard() {
        ListCard card = new ListCard("Vaccination alerts",
                "Overdue and due within " + clinic.vaccinations().getReminderDays() + " days",
                "Nothing outstanding",
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
        return card;
    }

    private ListCard buildUpcomingCard() {
        ListCard card = new ListCard("Upcoming appointments",
                "Scheduled visits from today onwards",
                "Nothing scheduled",
                "Book a visit from the Appointments screen.");

        for (Appointment appointment : clinic.reports().upcomingAppointments(MAX_LIST_ROWS)) {
            card.addRow("clock", Theme.c().info,
                    clinic.pets().petName(appointment.getPetId()),
                    appointment.getReason() + "  ·  "
                            + clinic.veterinarians().vetName(appointment.getVeterinarianId()),
                    DateUtil.format(appointment.getAppointmentDate()),
                    null);
        }
        card.refresh();
        return card;
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private SectionHeader section(String title, String subtitle) {
        return subtitle == null ? new SectionHeader(title) : new SectionHeader(title, subtitle);
    }
}
