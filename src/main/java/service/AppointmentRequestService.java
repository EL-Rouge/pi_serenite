package service;

import models.AppointmentRequest;
import models.ProposedDate;
import repository.AppointmentRequestRepository;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public class AppointmentRequestService {

    private final AppointmentRequestRepository repository = new AppointmentRequestRepository();

    public AppointmentRequest createAppointment(AppointmentRequest appointment) throws SQLException {
        // ← business rules live here, not in the repository
        validateAppointment(appointment);

        appointment.setStatus("PENDING");
        appointment.setCreationDate(LocalDateTime.now());

        AppointmentRequest saved = repository.save(appointment);
        repository.saveProposedDates(saved.getId(), appointment.getProposedDates());

        return saved;
    }

    public AppointmentRequest getById(long id) throws SQLException {
        AppointmentRequest app = repository.findById(id);
        if (app == null)
            throw new IllegalArgumentException("Appointment not found with id: " + id);
        return app;
    }

    public List<AppointmentRequest> getAllAppointments() throws SQLException {
        return repository.findAll();
    }

    public List<AppointmentRequest> getAppointmentsByClientId(long clientId) throws SQLException {
        return repository.findByClientId(clientId);
    }

    public AppointmentRequest confirmAppointment(long id, LocalDateTime confirmedDate) throws SQLException {
        AppointmentRequest app = getById(id);

        if (!app.getStatus().equals("PENDING")) {
            throw new IllegalStateException("Only PENDING appointments can be confirmed.");
        }

        app.setStatus("CONFIRMED");
        app.setConfirmedDate(confirmedDate);
        repository.update(app);

        return app;
    }

    public AppointmentRequest rescheduleAppointment(long id, List<ProposedDate> newDates) throws SQLException {
        AppointmentRequest app = getById(id);

        app.setStatus("PENDING");
        app.setConfirmedDate(null);
        app.setProposedDates(newDates);

        repository.update(app);
        repository.deleteProposedDates(id);
        repository.saveProposedDates(id, newDates);

        return app;
    }

    public void cancelAppointment(long id) throws SQLException {
        repository.delete(id);
    }

    // ─── Validation ─────────────────────────────────────────────

    private void validateAppointment(AppointmentRequest appointment) {
        if (appointment.getClientId() <= 0)
            throw new IllegalArgumentException("Invalid client ID.");
        if (appointment.getDoctorId() <= 0)
            throw new IllegalArgumentException("Invalid doctor ID.");
        if (appointment.getProposedDates() == null || appointment.getProposedDates().isEmpty())
            throw new IllegalArgumentException("At least one proposed date is required.");
        if (appointment.getType() == null || appointment.getType().isBlank())
            throw new IllegalArgumentException("Appointment type is required.");
    }

    public AppointmentRequest updateAppointment(AppointmentRequest app) throws SQLException {
        validateAppointment(app);
        repository.update(app);
        repository.deleteProposedDates(app.getId());
        repository.saveProposedDates(app.getId(), app.getProposedDates());
        return app;
    }


    /**
     * Flips the status of an appointment.
     * Called by ConsultationService after a consultation is saved (CONFIRMED → CONSULTED).
     */
    public void updateStatus(long id, String newStatus) throws SQLException {
        AppointmentRequest app = getById(id);
        app.setStatus(newStatus);
        repository.update(app);
    }



    /**
     * Get all appointments for a specific doctor.
     * Paste this inside the AppointmentRequestService class.
     */
    public List<AppointmentRequest> getAppointmentsByDoctorId(long doctorId) throws SQLException {
        return repository.findByDoctorId(doctorId);
    }

    /**
     * Refuse an appointment — sets status to REFUSED.
     * Paste this inside the AppointmentRequestService class.
     */
    public void refuseAppointment(long id) throws SQLException {
        AppointmentRequest app = getById(id);
        if (!app.getStatus().equalsIgnoreCase("PENDING")) {
            throw new IllegalStateException("Only PENDING appointments can be refused.");
        }
        app.setStatus("REFUSED");
        repository.update(app);
    }
    
}