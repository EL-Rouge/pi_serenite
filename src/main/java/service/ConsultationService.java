package service;

import models.AppointmentRequest;
import models.Consultation;
import repository.AppointmentRequestRepository;
import repository.ConsultationRepository;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public class ConsultationService {

    private final ConsultationRepository      repository            = new ConsultationRepository();
    private final AppointmentRequestRepository appointmentRepository = new AppointmentRequestRepository();

    // ─── CREATE ──────────────────────────────────────────────────

    /**
     * Creates a consultation only if the linked appointment exists and is CONFIRMED.
     */
    public Consultation createConsultation(Consultation consultation) throws SQLException {
        validateAppointmentIsConfirmed(consultation.getAppointmentRequestId());
        validateConsultation(consultation);

        consultation.setCreationDate(LocalDateTime.now());
        return repository.save(consultation);
    }

    // ─── READ ────────────────────────────────────────────────────

    public Consultation getById(long id) throws SQLException {
        Consultation c = repository.findById(id);
        if (c == null)
            throw new IllegalArgumentException("Consultation not found with id: " + id);
        return c;
    }

    public List<Consultation> getAllConsultations() throws SQLException {
        return repository.findAll();
    }

    public List<Consultation> getByClientId(long clientId) throws SQLException {
        return repository.findByClientId(clientId);
    }

    public List<Consultation> getByDoctorId(long doctorId) throws SQLException {
        return repository.findByDoctorId(doctorId);
    }

    public List<Consultation> getByAppointmentRequestId(long appointmentRequestId) throws SQLException {
        return repository.findByAppointmentRequestId(appointmentRequestId);
    }

    // ─── UPDATE ──────────────────────────────────────────────────

    public Consultation updateConsultation(Consultation consultation) throws SQLException {
        // Make sure the record exists first
        getById(consultation.getId());
        // Re-validate the appointment guard in case it changed
        validateAppointmentIsConfirmed(consultation.getAppointmentRequestId());
        validateConsultation(consultation);

        repository.update(consultation);
        return consultation;
    }

    // ─── DELETE ──────────────────────────────────────────────────

    public void deleteConsultation(long id) throws SQLException {
        // Make sure the record exists before deleting
        getById(id);
        repository.delete(id);
    }

    // ─── Guards & Validation ─────────────────────────────────────

    /**
     * Throws IllegalStateException if the appointment does not exist or is not CONFIRMED.
     * This is the central enforcement point for the "consultation requires confirmed appointment" rule.
     */
    private void validateAppointmentIsConfirmed(long appointmentRequestId) throws SQLException {
        AppointmentRequest appointment = appointmentRepository.findById(appointmentRequestId);

        if (appointment == null)
            throw new IllegalArgumentException(
                    "Appointment not found with id: " + appointmentRequestId);

        if (!"CONFIRMED".equals(appointment.getStatus()))
            throw new IllegalStateException(
                    "A consultation can only be created for a CONFIRMED appointment. " +
                            "Current status: " + appointment.getStatus());
    }

    private void validateConsultation(Consultation consultation) {
        if (consultation.getClientId() <= 0)
            throw new IllegalArgumentException("Invalid client ID.");
        if (consultation.getDoctorId() <= 0)
            throw new IllegalArgumentException("Invalid doctor ID.");
        if (consultation.getConsultationDate() == null)
            throw new IllegalArgumentException("Consultation date is required.");
        if (consultation.getDiagnosis() == null || consultation.getDiagnosis().isBlank())
            throw new IllegalArgumentException("Diagnosis is required.");
    }
}