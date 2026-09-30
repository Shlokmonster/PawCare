package com.pawcare.service;

import com.pawcare.exception.InvalidAppointmentException;
import com.pawcare.model.Appointment;
import com.pawcare.model.enums.AppointmentStatus;
import com.pawcare.util.IDGenerator;
import com.pawcare.util.ValidationUtil;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Scheduling service for veterinary visits.
 *
 * <p>The schedule is held in a {@code TreeMap<LocalDate, ArrayList<Appointment>>}.</p>
 *
 * <p>A TreeMap keeps its keys in their natural order at all times, so the appointment
 * calendar is always sorted by date without a single explicit sort call. Iterating the
 * map walks the days from earliest to latest, and asking for a range of days is a
 * {@code subMap} operation rather than a filtered scan. Each day holds an
 * {@code ArrayList} of visits, which is exactly the "one date, many appointments"
 * shape of a clinic diary.</p>
 *
 * <p>A {@code HashMap} keeps the same appointments addressable by id for edit, delete
 * and status changes. Every mutation rebuilds the TreeMap index from the HashMap, which
 * keeps the two views consistent however a record changes.</p>
 */
public class AppointmentService {

    /** Index by id – used by update, delete and status changes. */
    private final HashMap<String, Appointment> byId = new HashMap<>();

    /** The schedule: date -> appointments on that date, automatically date-ordered. */
    private final TreeMap<LocalDate, ArrayList<Appointment>> byDate = new TreeMap<>();

    /** Default order within a day. */
    private static final Comparator<Appointment> BY_TIME =
            Comparator.comparing(Appointment::getAppointmentTime,
                    Comparator.nullsLast(Comparator.naturalOrder()));

    // CREATE ----------------------------------------------------------

    public void add(Appointment appointment) throws InvalidAppointmentException {
        ValidationUtil.validateAppointment(appointment);

        if (byId.containsKey(appointment.getAppointmentId())) {
            throw new InvalidAppointmentException("Unable to save appointment.",
                    List.of("Appointment id " + appointment.getAppointmentId() + " already exists."));
        }

        if (hasConflict(appointment)) {
            throw new InvalidAppointmentException("Unable to save appointment.",
                    List.of("This veterinarian already has an appointment at that date and time."));
        }

        byId.put(appointment.getAppointmentId(), appointment);
        reindex();
        IDGenerator.sync(IDGenerator.APPOINTMENT_PREFIX, appointment.getAppointmentId());
    }

    // READ ------------------------------------------------------------

    public Optional<Appointment> findById(String appointmentId) {
        if (appointmentId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(byId.get(appointmentId.trim()));
    }

    /**
     * Every appointment in date order – obtained simply by walking the TreeMap, which
     * is the clearest demonstration of what the sorted map buys us.
     */
    public List<Appointment> getAll() {
        List<Appointment> result = new ArrayList<>();
        for (Map.Entry<LocalDate, ArrayList<Appointment>> entry : byDate.entrySet()) {
            List<Appointment> sameDay = new ArrayList<>(entry.getValue());
            sameDay.sort(BY_TIME);
            result.addAll(sameDay);
        }
        return result;
    }

    /** The distinct dates that currently have at least one appointment, oldest first. */
    public List<LocalDate> scheduledDates() {
        return new ArrayList<>(byDate.keySet());
    }

    /** Appointments on one day, ordered by time. */
    public List<Appointment> on(LocalDate date) {
        ArrayList<Appointment> sameDay = byDate.get(date);
        if (sameDay == null) {
            return new ArrayList<>();
        }
        List<Appointment> copy = new ArrayList<>(sameDay);
        copy.sort(BY_TIME);
        return copy;
    }

    public List<Appointment> today() {
        return on(LocalDate.now());
    }

    public List<Appointment> upcoming(int limit) {
        LocalDate today = LocalDate.now();
        return getAll().stream()
                .filter(a -> a.getStatus() == AppointmentStatus.SCHEDULED)
                .filter(a -> a.getAppointmentDate() != null && !a.getAppointmentDate().isBefore(today))
                .limit(limit)
                .collect(Collectors.toList());
    }

    public List<Appointment> byStatus(AppointmentStatus status) {
        return getAll().stream()
                .filter(a -> a.getStatus() == status)
                .collect(Collectors.toList());
    }

    public List<Appointment> byVeterinarian(String veterinarianId) {
        return getAll().stream()
                .filter(a -> veterinarianId == null || veterinarianId.equals(a.getVeterinarianId()))
                .collect(Collectors.toList());
    }

    public List<Appointment> byPet(String petId) {
        return getAll().stream()
                .filter(a -> petId != null && petId.equals(a.getPetId()))
                .collect(Collectors.toList());
    }

    /** The next scheduled visit for a pet, used by the pet profile screen. */
    public Optional<Appointment> nextForPet(String petId) {
        LocalDate today = LocalDate.now();
        return getAll().stream()
                .filter(a -> petId != null && petId.equals(a.getPetId()))
                .filter(a -> a.getStatus() == AppointmentStatus.SCHEDULED)
                .filter(a -> a.getAppointmentDate() != null && !a.getAppointmentDate().isBefore(today))
                .findFirst();
    }

    public Map<AppointmentStatus, Integer> countByStatus() {
        Map<AppointmentStatus, Integer> counts = new java.util.EnumMap<>(AppointmentStatus.class);
        for (AppointmentStatus status : AppointmentStatus.values()) {
            counts.put(status, 0);
        }
        for (Appointment appointment : byId.values()) {
            counts.merge(appointment.getStatus(), 1, Integer::sum);
        }
        return counts;
    }

    public int count() {
        return byId.size();
    }

    public int countForDate(LocalDate date) {
        ArrayList<Appointment> sameDay = byDate.get(date);
        return sameDay == null ? 0 : sameDay.size();
    }

    /**
     * Detects a double booking: the same veterinarian, on the same date, at the same
     * time, in a booking that is not cancelled. The appointment being validated is
     * excluded so an edit does not clash with itself.
     */
    public boolean hasConflict(Appointment candidate) {
        if (candidate == null || candidate.getAppointmentDate() == null
                || candidate.getAppointmentTime() == null || candidate.getVeterinarianId() == null) {
            return false;
        }
        if (candidate.getStatus() == AppointmentStatus.CANCELLED) {
            return false;
        }
        for (Appointment existing : byDate.getOrDefault(candidate.getAppointmentDate(), new ArrayList<>())) {
            if (existing.getAppointmentId().equals(candidate.getAppointmentId())) {
                continue;
            }
            if (existing.getStatus() == AppointmentStatus.CANCELLED) {
                continue;
            }
            boolean sameVet = candidate.getVeterinarianId().equals(existing.getVeterinarianId());
            boolean sameTime = candidate.getAppointmentTime().equals(existing.getAppointmentTime());
            if (sameVet && sameTime) {
                return true;
            }
        }
        return false;
    }

    // UPDATE / DELETE -------------------------------------------------

    public void update(Appointment appointment) throws InvalidAppointmentException {
        ValidationUtil.validateAppointment(appointment);

        if (!byId.containsKey(appointment.getAppointmentId())) {
            throw new InvalidAppointmentException("Unable to update appointment.",
                    List.of("Appointment id " + appointment.getAppointmentId() + " does not exist."));
        }

        if (hasConflict(appointment)) {
            throw new InvalidAppointmentException("Unable to update appointment.",
                    List.of("This veterinarian already has an appointment at that date and time."));
        }

        byId.put(appointment.getAppointmentId(), appointment);
        reindex();
    }

    /** Convenience transition used by the "Complete" row action. */
    public void markCompleted(String appointmentId) throws InvalidAppointmentException {
        Appointment appointment = byId.get(appointmentId);
        if (appointment == null) {
            throw new InvalidAppointmentException("Unable to update appointment.",
                    List.of("Appointment id " + appointmentId + " does not exist."));
        }
        appointment.setStatus(AppointmentStatus.COMPLETED);
        reindex();
    }

    /** Convenience transition used by the "Cancel" row action. */
    public void cancel(String appointmentId) throws InvalidAppointmentException {
        Appointment appointment = byId.get(appointmentId);
        if (appointment == null) {
            throw new InvalidAppointmentException("Unable to cancel appointment.",
                    List.of("Appointment id " + appointmentId + " does not exist."));
        }
        appointment.setStatus(AppointmentStatus.CANCELLED);
        reindex();
    }

    public boolean delete(String appointmentId) {
        if (appointmentId == null || byId.remove(appointmentId.trim()) == null) {
            return false;
        }
        reindex();
        return true;
    }

    /** Removes every appointment belonging to a pet – used when a pet is deleted. */
    public int deleteByPet(String petId) {
        List<String> doomed = byId.values().stream()
                .filter(a -> petId != null && petId.equals(a.getPetId()))
                .map(Appointment::getAppointmentId)
                .collect(Collectors.toList());
        doomed.forEach(byId::remove);
        reindex();
        return doomed.size();
    }

    /** Rebuilds the TreeMap index from the id map after any change. */
    private void reindex() {
        byDate.clear();
        for (Appointment appointment : byId.values()) {
            LocalDate date = appointment.getAppointmentDate();
            if (date == null) {
                continue;
            }
            byDate.computeIfAbsent(date, key -> new ArrayList<>()).add(appointment);
        }
    }

    /** Sorts an arbitrary list of appointments for display purposes. */
    public static List<Appointment> sort(List<Appointment> appointments, Comparator<Appointment> comparator) {
        List<Appointment> copy = new ArrayList<>(appointments);
        copy.sort(comparator);
        return copy;
    }

    /** Comparators exposed for the appointment page's sort control. */
    public static final Comparator<Appointment> BY_DATE_THEN_TIME =
            Comparator.comparing(Appointment::getAppointmentDate,
                            Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(Appointment::getAppointmentTime,
                            Comparator.nullsLast(Comparator.naturalOrder()));

    public static final Comparator<Appointment> BY_STATUS =
            Comparator.comparing(a -> a.getStatus() == null ? "" : a.getStatus().getLabel());

    public static final Comparator<Appointment> BY_VET =
            Comparator.comparing(a -> a.getVeterinarianId() == null ? "" : a.getVeterinarianId());

    // BULK ------------------------------------------------------------

    public void loadAll(List<Appointment> loaded) {
        byId.clear();
        if (loaded != null) {
            for (Appointment appointment : loaded) {
                if (appointment == null || appointment.getAppointmentId() == null) {
                    continue;
                }
                byId.put(appointment.getAppointmentId(), appointment);
            }
        }
        reindex();
        IDGenerator.sync(IDGenerator.APPOINTMENT_PREFIX,
                byId.keySet().stream().collect(Collectors.toList()));
    }

    /** Normalises a time so that seconds and nanos never break equality checks. */
    public static LocalTime normalise(LocalTime time) {
        return time == null ? null : time.withSecond(0).withNano(0);
    }
}
