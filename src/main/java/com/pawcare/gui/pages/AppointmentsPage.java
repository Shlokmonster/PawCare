package com.pawcare.gui.pages;

import com.pawcare.exception.PawCareException;
import com.pawcare.gui.Page;
import com.pawcare.gui.components.ActionColumn;
import com.pawcare.gui.components.BadgeRenderer;
import com.pawcare.gui.components.Card;
import com.pawcare.gui.components.Column;
import com.pawcare.gui.components.DateRenderer;
import com.pawcare.gui.components.ModernButton;
import com.pawcare.gui.components.ModernComboBox;
import com.pawcare.gui.components.ModernTable;
import com.pawcare.gui.components.SearchField;
import com.pawcare.gui.components.StatCard;
import com.pawcare.gui.dialogs.AppointmentDialog;
import com.pawcare.gui.dialogs.Dialogs;
import com.pawcare.model.Appointment;
import com.pawcare.model.enums.AppointmentStatus;
import com.pawcare.service.ClinicService;
import com.pawcare.theme.Theme;
import com.pawcare.util.DateUtil;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The clinic diary.
 *
 * <p>The rows come from the appointment service, which holds the schedule in a
 * {@code TreeMap<LocalDate, ArrayList<Appointment>>}. Walking that map already yields the
 * visits in date order, so the table is chronological before the user sorts it.</p>
 *
 * <p>Besides edit and delete, a booking can be completed or cancelled straight from its
 * row, which is the pair of transitions a reception desk performs all day.</p>
 */
public class AppointmentsPage extends Page {

    /** View index of the column holding the row actions. */
    private static final int ACTION_COLUMN = 7;
    /** Height of the table card. The page scrolls rather than squashing the table. */
    private static final int TABLE_HEIGHT = 420;

    private final SearchField search = new SearchField("Search by patient, veterinarian, reason or id");
    private final ModernComboBox<String> statusFilter = new ModernComboBox<>(
            new String[]{"All statuses", "Scheduled", "Completed", "Cancelled"});
    private final ModernComboBox<String> scopeFilter = new ModernComboBox<>(
            new String[]{"All dates", "Today", "Upcoming", "Past"});
    private final JLabel summary = new JLabel();
    private final ModernTable<Appointment> table;

    private final StatCard todayCard =
            new StatCard("Visits today", "0", "in the diary", "clock", Theme.c().info);
    private final StatCard upcomingCard =
            new StatCard("Upcoming visits", "0", "still scheduled", "appointments", Theme.c().primary);
    private final StatCard completedCard =
            new StatCard("Completed", "0", "visits closed", "check", Theme.c().success);
    private final StatCard cancelledCard =
            new StatCard("Cancelled", "0", "bookings dropped", "close", Theme.c().danger);

    public AppointmentsPage(ClinicService clinic) {
        super(clinic, "Appointments",
                "Every booked visit, in date order. Complete, cancel, reschedule or remove a booking.");

        ModernButton book = new ModernButton("Book appointment",
                ModernButton.Variant.PRIMARY, "plus");
        book.addActionListener(e -> openForm(null));
        header().addAction(book);

        table = new ModernTable<>(buildColumns());
        table.setEmptyIcon("appointments");
        table.setEmptyMessage("No appointments match the current filters");
        ActionColumn.install(table, ACTION_COLUMN, this::onRowAction,
                "Complete", "Cancel", "Edit", "Delete");

        search.onChange(this::applyFilters);
        statusFilter.addActionListener(e -> applyFilters());
        scopeFilter.addActionListener(e -> applyFilters());

        summary.setFont(Theme.FONT_TINY);
        summary.setForeground(Theme.c().textSecondary);

        stack(row(todayCard, upcomingCard, completedCard, cancelledCard), Theme.SPACE_XL);
        stack(buildToolbar(), Theme.SPACE_MD);
        stack(fixed(buildTableCard(), TABLE_HEIGHT), 0);
    }

    // ------------------------------------------------------------------
    // Static layout
    // ------------------------------------------------------------------

    private List<Column<Appointment>> buildColumns() {
        List<Column<Appointment>> columns = new ArrayList<>();
        columns.add(new Column<>("ID", Appointment::getAppointmentId, 70));
        columns.add(new Column<>("Date", Appointment::getAppointmentDate, 115, new DateRenderer()));
        columns.add(new Column<>("Time", Appointment::getAppointmentTime, 95, new DateRenderer()));
        columns.add(new Column<>("Patient",
                appointment -> clinic.pets().petName(appointment.getPetId()), 150));
        columns.add(new Column<>("Veterinarian",
                appointment -> clinic.veterinarians().vetName(appointment.getVeterinarianId()), 160));
        columns.add(new Column<>("Reason", Appointment::getReason, 230));
        columns.add(new Column<>("Status", Appointment::getStatus, 115, new BadgeRenderer()));
        columns.add(new Column<>("Actions", appointment -> "", 280));
        return columns;
    }

    private JComponent buildToolbar() {
        JPanel bar = new JPanel(new BorderLayout(Theme.SPACE_MD, 0));
        bar.setOpaque(false);
        bar.add(search, BorderLayout.CENTER);

        ModernButton clear = new ModernButton("Clear", ModernButton.Variant.SECONDARY, "refresh");
        clear.addActionListener(e -> {
            search.clear();
            statusFilter.setSelectedIndex(0);
            scopeFilter.setSelectedIndex(0);
            applyFilters();
        });

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.RIGHT, Theme.SPACE_SM, 0));
        filters.setOpaque(false);
        filters.add(statusFilter);
        filters.add(scopeFilter);
        filters.add(clear);
        bar.add(filters, BorderLayout.EAST);
        return bar;
    }

    private Card buildTableCard() {
        Card card = new Card("Diary",
                "Sorted by date and time from the schedule itself. Use the row actions to update a booking.",
                summary);
        card.content().add(scroll(table, 400), BorderLayout.CENTER);
        return card;
    }

    // ------------------------------------------------------------------
    // Data
    // ------------------------------------------------------------------

    @Override
    public void refresh() {
        refreshHeader();
        updateSummaryCards();
        applyFilters();
    }

    private void updateSummaryCards() {
        todayCard.setValue(String.valueOf(clinic.reports().todaysVisitCount()));
        todayCard.setCaption(DateUtil.formatLong(LocalDate.now()));

        int upcoming = clinic.appointments().upcoming(Integer.MAX_VALUE).size();
        upcomingCard.setValue(String.valueOf(upcoming));
        upcomingCard.setCaption(upcoming == 1 ? "visit still open" : "visits still open");

        int completed = clinic.appointments().byStatus(AppointmentStatus.COMPLETED).size();
        completedCard.setValue(String.valueOf(completed));

        int cancelled = clinic.appointments().byStatus(AppointmentStatus.CANCELLED).size();
        cancelledCard.setValue(String.valueOf(cancelled));

        int total = clinic.appointments().count();
        completedCard.setCaption(total == 0 ? "no bookings yet"
                : share(completed, total) + "% of all bookings");
        cancelledCard.setCaption(total == 0 ? "no bookings yet"
                : share(cancelled, total) + "% of all bookings");
    }

    private static String share(int part, int total) {
        return String.format(Locale.ENGLISH, "%.0f", part * 100.0 / total);
    }

    /** Applies the search box, the status filter and the date scope together. */
    private void applyFilters() {
        List<Appointment> rows = new ArrayList<>();
        for (Appointment appointment : clinic.appointments().getAll()) {
            if (matchesSearch(appointment) && matchesStatus(appointment) && matchesScope(appointment)) {
                rows.add(appointment);
            }
        }
        int total = clinic.appointments().count();
        summary.setText(rows.size() + " of " + total + (total == 1 ? " booking" : " bookings"));
        table.setRows(rows);
    }

    private boolean matchesSearch(Appointment appointment) {
        String query = search.getText();
        if (query == null || query.isBlank()) {
            return true;
        }
        String needle = query.trim().toLowerCase(Locale.ENGLISH);
        return contains(clinic.pets().petName(appointment.getPetId()), needle)
                || contains(clinic.veterinarians().vetName(appointment.getVeterinarianId()), needle)
                || contains(appointment.getReason(), needle)
                || contains(appointment.getAppointmentId(), needle)
                || contains(appointment.getNotes(), needle);
    }

    private boolean matchesStatus(Appointment appointment) {
        switch (statusFilter.getSelectedIndex()) {
            case 1:
                return appointment.getStatus() == AppointmentStatus.SCHEDULED;
            case 2:
                return appointment.getStatus() == AppointmentStatus.COMPLETED;
            case 3:
                return appointment.getStatus() == AppointmentStatus.CANCELLED;
            default:
                return true;
        }
    }

    private boolean matchesScope(Appointment appointment) {
        LocalDate date = appointment.getAppointmentDate();
        if (date == null) {
            return scopeFilter.getSelectedIndex() == 0;
        }
        LocalDate today = LocalDate.now();
        switch (scopeFilter.getSelectedIndex()) {
            case 1:
                return date.isEqual(today);
            case 2:
                return !date.isBefore(today) && appointment.getStatus() == AppointmentStatus.SCHEDULED;
            case 3:
                return date.isBefore(today);
            default:
                return true;
        }
    }

    private static boolean contains(String value, String needle) {
        return value != null && value.toLowerCase(Locale.ENGLISH).contains(needle);
    }

    // ------------------------------------------------------------------
    // Actions
    // ------------------------------------------------------------------

    private void onRowAction(String action, int modelRow) {
        Appointment appointment = table.getEntityModel().getRow(modelRow);
        if (appointment == null) {
            return;
        }
        switch (action) {
            case "Complete":
                complete(appointment);
                break;
            case "Cancel":
                cancel(appointment);
                break;
            case "Edit":
                openForm(appointment);
                break;
            case "Delete":
                delete(appointment);
                break;
            default:
                break;
        }
    }

    private void complete(Appointment appointment) {
        if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
            Dialogs.info(this, "Already completed",
                    "This visit is already marked as completed.");
            return;
        }
        if (!Dialogs.confirm(this, "Complete visit",
                "Mark the visit for " + clinic.pets().petName(appointment.getPetId())
                        + " on " + DateUtil.format(appointment.getAppointmentDate(),
                        appointment.getAppointmentTime()) + " as completed?",
                "Mark completed")) {
            return;
        }
        try {
            clinic.completeAppointment(appointment.getAppointmentId());
            refresh();
            toast("Visit marked as completed");
        } catch (PawCareException e) {
            Dialogs.error(this, "Unable to complete the visit", e);
        }
    }

    private void cancel(Appointment appointment) {
        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            Dialogs.info(this, "Already cancelled",
                    "This booking has already been cancelled.");
            return;
        }
        if (!Dialogs.confirm(this, "Cancel booking",
                "Cancel the visit for " + clinic.pets().petName(appointment.getPetId())
                        + " on " + DateUtil.format(appointment.getAppointmentDate(),
                        appointment.getAppointmentTime())
                        + "?\nThe record is kept so the history stays complete.",
                "Cancel booking")) {
            return;
        }
        try {
            clinic.cancelAppointment(appointment.getAppointmentId());
            refresh();
            toast("Booking cancelled");
        } catch (PawCareException e) {
            Dialogs.error(this, "Unable to cancel the booking", e);
        }
    }

    private void openForm(Appointment existing) {
        if (clinic.pets().count() == 0 || clinic.veterinarians().count() == 0) {
            Dialogs.warning(this, "Cannot book a visit",
                    "Register at least one patient before booking an appointment.");
            return;
        }
        AppointmentDialog dialog = new AppointmentDialog(window(), clinic, existing);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            refresh();
            toast(existing == null ? "Appointment booked" : "Appointment updated");
        }
    }

    private void delete(Appointment appointment) {
        String subject = "the booking for " + clinic.pets().petName(appointment.getPetId())
                + " on " + DateUtil.format(appointment.getAppointmentDate(),
                appointment.getAppointmentTime());
        if (!Dialogs.confirmDelete(this, subject)) {
            return;
        }
        try {
            clinic.deleteAppointment(appointment.getAppointmentId());
            refresh();
            toast("Appointment deleted");
        } catch (PawCareException e) {
            Dialogs.error(this, "Unable to delete the appointment", e);
        }
    }
}
