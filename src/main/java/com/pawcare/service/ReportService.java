package com.pawcare.service;

import com.pawcare.model.Appointment;
import com.pawcare.model.Owner;
import com.pawcare.model.Pet;
import com.pawcare.model.Treatment;
import com.pawcare.model.Vaccination;
import com.pawcare.model.enums.AppointmentStatus;
import com.pawcare.model.enums.Species;
import com.pawcare.model.enums.VaccinationStatus;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Computes every number shown on the dashboard and the reports page.
 *
 * <p>Nothing here is hard-coded: each figure is derived from the live collections held
 * by the other services. The class demonstrates composition again – a report is simply
 * a different <i>view</i> over data that already exists elsewhere.</p>
 */
public class ReportService {

    private final PetService petService;
    private final OwnerService ownerService;
    private final VeterinarianService veterinarianService;
    private final AppointmentService appointmentService;
    private final TreatmentService treatmentService;
    private final VaccinationService vaccinationService;

    public ReportService(PetService petService,
                         OwnerService ownerService,
                         VeterinarianService veterinarianService,
                         AppointmentService appointmentService,
                         TreatmentService treatmentService,
                         VaccinationService vaccinationService) {
        this.petService = petService;
        this.ownerService = ownerService;
        this.veterinarianService = veterinarianService;
        this.appointmentService = appointmentService;
        this.treatmentService = treatmentService;
        this.vaccinationService = vaccinationService;
    }

    // Totals ----------------------------------------------------------

    public int totalPets() {
        return petService.count();
    }

    public int totalOwners() {
        return ownerService.count();
    }

    public int totalVeterinarians() {
        return veterinarianService.count();
    }

    public int totalAppointments() {
        return appointmentService.count();
    }

    public int totalTreatments() {
        return treatmentService.count();
    }

    public int totalVaccinations() {
        return vaccinationService.count();
    }

    // Breakdowns ------------------------------------------------------

    public Map<Species, Integer> petsBySpecies() {
        return petService.countBySpecies();
    }

    public Map<AppointmentStatus, Integer> appointmentsByStatus() {
        return appointmentService.countByStatus();
    }

    public Map<VaccinationStatus, Integer> vaccinationsByStatus() {
        return vaccinationService.countByStatus();
    }

    // Operational views -----------------------------------------------

    /** Visits booked for today, whatever their status. */
    public List<Appointment> todaysAppointments() {
        return appointmentService.today();
    }

    public int todaysVisitCount() {
        return appointmentService.today().size();
    }

    /** Scheduled visits that have not happened yet. */
    public List<Appointment> upcomingAppointments(int limit) {
        return appointmentService.upcoming(limit);
    }

    public List<Treatment> recentTreatments(int limit) {
        return treatmentService.recent(limit);
    }

    /** Overdue and due-soon vaccinations, most urgent first. */
    public List<Vaccination> vaccinationAlerts() {
        return vaccinationService.alerts();
    }

    public int vaccinationAlertCount() {
        return vaccinationService.alerts().size();
    }

    // Ratio helpers used by the charts ---------------------------------

    /** Percentage of registered pets that are of the given species, rounded to a whole number. */
    public int speciesShare(Species species) {
        int total = totalPets();
        if (total == 0) {
            return 0;
        }
        int count = petsBySpecies().getOrDefault(species, 0);
        return Math.round(count * 100f / total);
    }

    /** Percentage of appointments in the given state. */
    public int appointmentShare(AppointmentStatus status) {
        int total = totalAppointments();
        if (total == 0) {
            return 0;
        }
        return Math.round(appointmentsByStatus().getOrDefault(status, 0) * 100f / total);
    }

    /**
     * Owners ranked by how many pets they have registered.
     *
     * <p>Uses the classic {@link Collections#sort} call to order the entries, which is
     * the older sibling of {@code List.sort} used elsewhere in the project.</p>
     */
    public List<Map.Entry<Owner, Integer>> topOwnersByPetCount(int limit) {
        List<Map.Entry<Owner, Integer>> rows = new ArrayList<>();
        for (Owner owner : ownerService.getAllOwners()) {
            rows.add(Map.entry(owner, petService.findByOwner(owner.getOwnerId()).size()));
        }
        Collections.sort(rows, (a, b) -> Integer.compare(b.getValue(), a.getValue()));
        return rows.subList(0, Math.min(limit, rows.size()));
    }

    /** The number of visits recorded in the last seven days. */
    public int visitsThisWeek() {
        LocalDate cutoff = LocalDate.now().minusDays(7);
        return (int) appointmentService.getAll().stream()
                .filter(a -> a.getAppointmentDate() != null && !a.getAppointmentDate().isBefore(cutoff))
                .count();
    }

    /** Overall health indicator used by the dashboard summary strip. */
    public String clinicStatusLine() {
        int overdue = vaccinationService.countOverdue();
        if (overdue > 0) {
            return overdue + (overdue == 1 ? " pet has an overdue vaccination" : " pets have overdue vaccinations");
        }
        int today = todaysVisitCount();
        return today == 0 ? "No visits booked for today" : today + " visits booked today";
    }
}
